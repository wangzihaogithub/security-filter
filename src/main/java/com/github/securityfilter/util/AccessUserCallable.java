package com.github.securityfilter.util;

import java.util.concurrent.Callable;

public class AccessUserCallable<V> implements java.util.concurrent.Callable<V> {
    private final AccessUserSnapshot snapshot = AccessUserUtil.getSnapshot();
    private final java.util.concurrent.Callable<V> source;

    public AccessUserCallable(java.util.concurrent.Callable<V> callable) {
        this.source = callable;
    }

    public AccessUserSnapshot getSnapshot() {
        return snapshot;
    }

    public Callable<V> getSource() {
        return source;
    }

    @Override
    public V call() throws Exception {
        try (AccessUserSnapshot open = snapshot.fork()) {
            return source.call();
        }
    }
}