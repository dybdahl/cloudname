package org.cloudname.zk;

import org.cloudname.StorageOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class ZkStorageFuture implements StorageOperation {
    private boolean isDone = false;
    List<Future> futures = Collections.synchronizedList(new ArrayList());
    final String errorMessage;
    
    public ZkStorageFuture() {
        errorMessage = null;
    }

    public ZkStorageFuture(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public boolean waitForCompletionMillis(int milliSeconds) {
        synchronized (this) {
            if (isDone()) {
                return true;
            }
            if (errorMessage != null) {
                return false;
            }
        }
        final CountDownLatch latch = new CountDownLatch(1);
        registerCallback(new Future() {
            @Override
            public void success() {
                latch.countDown();
            }

            @Override
            public void failure(String message) {
                // not used
            }

        });
        try {
            return latch.await(milliSeconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void registerCallback(Future future) {
        if (errorMessage !=  null) {
            future.failure(errorMessage);
            return;
        }

        boolean runCallback;
        synchronized (this) {
            runCallback = isDone;
            if (!runCallback) {
                futures.add(future);
            }
        }
        if (runCallback)  {
            future.success();
        }
    }

    @Override
    public boolean isDone() {
        synchronized (this) {
            return isDone;
        }
    }

    public Future getSystemCallback() {

        return new Future() {
            @Override
            public void success() {
                synchronized (this) {
                    if (isDone) {
                        return;
                    }
                    isDone = true;
                }
                for (Future future : futures) {
                    future.success();
                }
            }

            @Override
            public void failure(String message) {
                // not used
            }
        };
    }
}