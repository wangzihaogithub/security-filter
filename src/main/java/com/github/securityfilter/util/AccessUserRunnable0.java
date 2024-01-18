package com.github.securityfilter.util;

public class AccessUserRunnable0 implements AccessUserUtil.Runnable {
    private final AccessUserSnapshot snapshot = AccessUserUtil.getSnapshot();
    private final AccessUserUtil.Runnable source;

    public AccessUserRunnable0(AccessUserUtil.Runnable runnable) {
        this.source = runnable;
    }

    public AccessUserSnapshot getSnapshot() {
        return snapshot;
    }

    public AccessUserUtil.Runnable getSource() {
        return source;
    }

    @Override
    public void run() throws Throwable {
        try (AccessUserSnapshot open = snapshot.fork()) {
            source.run();
        }
    }
}