package com.egeio.opencv.work;

public abstract class Worker implements Runnable {

    private boolean isWorkerStopped = false;

    public final synchronized void stopWork() {
        isWorkerStopped = true;
    }

    public final synchronized boolean isWorkerStopped() {
        return isWorkerStopped;
    }

    public abstract void doWork();

    @Override
    public final void run() {
        doWork();
    }

    protected void assertWorkStopped() {
        if (isWorkerStopped()) {
            throw new WorkStoppedException("Work has been stopped");
        }
    }

    public static class WorkStoppedException extends IllegalStateException {
        public WorkStoppedException() {
        }

        public WorkStoppedException(String s) {
            super(s);
        }

        public WorkStoppedException(String message, Throwable cause) {
            super(message, cause);
        }

        public WorkStoppedException(Throwable cause) {
            super(cause);
        }
    }
}