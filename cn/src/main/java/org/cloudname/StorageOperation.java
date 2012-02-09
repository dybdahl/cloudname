package org.cloudname;


public interface StorageOperation {
    boolean waitForCompletionMillis(int milliSeconds);

    public interface Callback {
        void success();
        void failure(String message);
    }

    public void registerCallback(Callback callback);

    public boolean isDone();
}