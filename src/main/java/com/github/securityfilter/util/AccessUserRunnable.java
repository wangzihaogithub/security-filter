package com.github.securityfilter.util;

public class AccessUserRunnable implements java.lang.Runnable {
    private final AccessUserSnapshot snapshot = AccessUserUtil.getSnapshot();
    private final java.lang.Runnable source;

    public AccessUserRunnable(java.lang.Runnable runnable) {
        this.source = runnable;
    }

    public AccessUserSnapshot getSnapshot() {
        return snapshot;
    }

    public Runnable getSource() {
        return source;
    }

    @Override
    public void run() {
        try (AccessUserSnapshot open = snapshot.fork()) {
            source.run();
        }
    }
}