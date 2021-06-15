package com.aservo.ldap.adapter.api.cursor;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;


public abstract class MappableCursor<T>
        implements Cursor<T> {

    public abstract boolean next();

    public abstract T get();

    public <R> MappableCursor<R> map(Function<T, R> f) {

        return new MappableCursor<R>() {

            @Override
            public boolean next() {

                return MappableCursor.this.next();
            }

            @Override
            public R get() {

                return f.apply(MappableCursor.this.get());
            }

            @Override
            public void close()
                    throws IOException {

                MappableCursor.this.close();
            }
        };
    }

    @Override
    public Iterator<T> iterator() {

        return iterator(Function.identity());
    }

    public <R> ClosableIterator<R> iterator(Function<T, R> f) {

        return new ClosableIterator<R>() {

            private boolean initialized = false;
            private boolean hasMore;

            @Override
            public boolean hasNext() {

                if (!initialized) {

                    hasMore = MappableCursor.this.next();
                    initialized = true;
                }

                return hasMore;
            }

            @Override
            public R next() {

                if (!initialized) {

                    hasMore = MappableCursor.this.next();
                }

                if (!hasMore) {

                    throw new NoSuchElementException("There is no element left for further iterations.");
                }

                initialized = false;

                return f.apply(MappableCursor.this.get());
            }

            @Override
            public void close()
                    throws IOException {

                MappableCursor.this.close();
            }
        };
    }

    @Override
    public void close()
            throws IOException {
    }

    public static <T> MappableCursor<T> fromIterable(Iterable<T> iterable) {

        return fromIterator(iterable.iterator());
    }

    public static <T> MappableCursor<T> fromIterator(Iterator<T> iterator) {

        return new MappableCursor<T>() {

            private T element;

            @Override
            public boolean next() {

                if (iterator.hasNext())
                    element = iterator.next();
                else
                    element = null;

                return element != null;
            }

            @Override
            public T get() {

                if (element == null)
                    throw new NoSuchElementException("There is no element for iterations.");

                return element;
            }

            @Override
            public void close()
                    throws IOException {

                if (iterator instanceof ClosableIterator)
                    ((ClosableIterator) iterator).close();
            }
        };
    }

    public static <T, C extends Cursor<T>> MappableCursor<T> flatten(C... cursors) {

        return flatten(Arrays.asList(cursors));
    }

    public static <T, C extends Cursor<T>> MappableCursor<T> flatten(Collection<C> cursors) {

        List<Cursor<T>> cursorList = new ArrayList<>(cursors);

        return new MappableCursor<T>() {

            int index = 0;

            @Override
            public boolean next() {

                for (; index < cursorList.size(); index++) {

                    if (cursorList.get(index).next())
                        return true;
                }

                return false;
            }

            @Override
            public T get() {

                return cursorList.get(index).get();
            }

            @Override
            public void close()
                    throws IOException {

                for (Cursor<T> cursor : cursorList)
                    cursor.close();
            }
        };
    }
}
