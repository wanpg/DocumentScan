package com.egeio.opencv;

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
}