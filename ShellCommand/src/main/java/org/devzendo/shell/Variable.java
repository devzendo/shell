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

public final class Variable {
    final ArrayList<Object> mStore = new ArrayList<Object>();

    public int size() {
        synchronized (mStore) {
            return mStore.size();
        }
    }
    
    public void add(final Object obj) {
        synchronized (mStore) {
            mStore.add(obj);
        }
    }
    
    public Object get(int index) {
        synchronized (mStore) {
            return mStore.get(index);
        }
    }
}
