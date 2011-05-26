/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devzendo.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ExecutionContainer {

    private final List<CommandHandler> mCommandHandlers;

    public ExecutionContainer(List<CommandHandler> commandHandlers) {
        mCommandHandlers = commandHandlers;
    }

    public void execute() throws CommandExecutionException {
        if (mCommandHandlers.size() == 1) {
            executeOnCurrentThread();
        } else {
            executeOnMultipleThreads();
        }
        terminatePipes();
    }

    private void terminatePipes() {
        for (CommandHandler handler: mCommandHandlers) {
            if (handler.getInputPipe() != null) {
                handler.getInputPipe().setTerminated();
            }
            if (handler.getOutputPipe() != null) {
                handler.getOutputPipe().setTerminated();
            }
        }
    }

    private void executeOnMultipleThreads() throws CommandExecutionException {
        final CountDownLatch latch = new CountDownLatch(mCommandHandlers.size());
        final List<CommandExecutionException> exceptions = Collections.synchronizedList(new ArrayList<CommandExecutionException>());
        for (CommandHandler handler: mCommandHandlers) {
            final CommandHandler finalHandler = handler;
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        finalHandler.execute();
                    } catch (CommandExecutionException e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                }});
            thread.setName(handler.getName());
            thread.start();
        }
        try {
            latch.await();
        } catch (final InterruptedException e) {
            throw new CommandExecutionException("Wait for commands to finish executing was interrupted: " + e.getMessage());
        }
        
        if (exceptions.size() == 0) {
            return;
        }
        
        final StringBuilder sb = new StringBuilder();
        sb.append("Multiple commands failed: ");
        for (int i = 0; i < exceptions.size(); i++) {
            sb.append(exceptions.get(i).getMessage());
            if (i != exceptions.size() - 1) {
                sb.append("; ");
            }
        }
        throw new CommandExecutionException(sb.toString());
    }

    private void executeOnCurrentThread() throws CommandExecutionException {
        mCommandHandlers.get(0).execute();
    }
}
