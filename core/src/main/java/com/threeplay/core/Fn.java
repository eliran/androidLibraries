package com.threeplay.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 10/14/16.
 */

public abstract class Fn<T> {
    public static <T> Fn<T> from(Iterable<T> values){
        return new FnIterator<>(values.iterator());
    }

    public abstract boolean hasNext();

    public abstract T next();

    public Fn<T> filter(final Predicate<T> predicate){
        return new Fn<T>() {{
                skipInvalidValues();
            }

            private T nextValue;
            private boolean nextIsValid;

            private void skipInvalidValues(){
                nextIsValid = false;
                while ( Fn.this.hasNext() ) {
                    nextValue = Fn.this.next();
                    nextIsValid = predicate.withValue(nextValue);
                    if ( nextIsValid ) break;
                }
            }

            @Override
            public boolean hasNext() {
                return nextIsValid;
            }

            @Override
            public T next() {
                T next = nextValue;
                skipInvalidValues();
                return next;
            }
        };
    }

    public <U> Fn<U> map(final Mapper<T, U> mapper){
        return new Fn<U>() {
            @Override
            public boolean hasNext() {
                return Fn.this.hasNext();
            }

            @Override
            public U next() {
                return mapper.fromValue(Fn.this.next());
            }
        };
    }

    public T reduce(final Reducer<T, T> reducer){
        return hasNext() ? reduce(next(), reducer) : null;
    }

    public <U> U reduce(U initial, final Reducer<T, U> reducer){
        while ( hasNext() ) {
           initial = reducer.withValues(initial, next());
        }
        return initial;
    }

    public List<T> toList() {
        return appendList(new LinkedList<T>());
    }

    public List<T> appendList(List<T> list){
        while ( hasNext() ) {
            list.add(next());
        }
        return list;
    }

    public static <U extends Number> Predicate<U> evenNumbers(){
        return new Predicate<U>(){
            @Override
            public boolean withValue(U value) {
                return value.intValue() % 2 == 0;
            }
        };
    }

    public static <U extends Number> Predicate<U> oddNumbers(){
        return new Predicate<U>(){
            @Override
            public boolean withValue(U value) {
                return value.intValue() % 2 == 1;
            }
        };
    }

    public static Reducer<Integer, Integer> sum(){
        return new Reducer<Integer, Integer>() {
            @Override
            public Integer withValues(Integer result, Integer value) {
                return result + value;
            }
        };
    }

    public static <T> Reducer<T, String> concat(){
        return new Reducer<T, String>() {
            @Override
            public String withValues(String result, T value) {
                return result + value;
            }
        };
    }

    interface Predicate<T> {
        boolean withValue(T value);
    }

    public interface Mapper<T, U> {
        U fromValue(T value);
    }

    interface Reducer<T, U> {
        U withValues(U result, T value);
    }

    static class FnIterator<T> extends Fn {
        private Iterator<T> iterator;

        public FnIterator(Iterator<T> iterator){
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }
    }
}
