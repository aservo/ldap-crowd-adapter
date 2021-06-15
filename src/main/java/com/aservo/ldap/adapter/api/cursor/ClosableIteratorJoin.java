/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aservo.ldap.adapter.api.cursor;

import java.io.IOException;
import java.util.*;


public class ClosableIteratorJoin<T>
        implements ClosableIterator<T> {

    private final List<Iterator<T>> iterators = new ArrayList<>();
    int index = 0;

    public ClosableIteratorJoin(Iterator<T>... iterators) {

        this.iterators.addAll(Arrays.asList(iterators));
    }

    public ClosableIteratorJoin(Collection<Iterator<T>> iterators) {

        this.iterators.addAll(iterators);
    }

    @Override
    public boolean hasNext() {

        return getCurrentIterator().hasNext();
    }

    @Override
    public T next() {

        return getCurrentIterator().next();
    }

    @Override
    public void close()
            throws IOException {

        for (Iterator<T> iter : iterators)
            if (iter instanceof ClosableIterator)
                ((ClosableIterator<T>) iter).close();
    }

    private Iterator<T> getCurrentIterator() {

        for (; index < iterators.size(); index++) {

            Iterator<T> iter = iterators.get(index);

            if (iter.hasNext())
                return iter;
        }

        return iterators.get(iterators.size() - 1);
    }
}
