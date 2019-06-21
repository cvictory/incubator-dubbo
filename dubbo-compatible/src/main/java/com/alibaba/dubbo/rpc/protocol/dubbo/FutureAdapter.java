package com.alibaba.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.rpc.AppResponse;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * 2019-06-20
 */
@Deprecated
public class FutureAdapter<V> implements Future<V> {
    private CompletableFuture<V> future;

    public FutureAdapter(CompletableFuture<V> future) {
        this.future = future;

    }

    public ResponseFuture getFuture() {
        return new ResponseFuture() {
            @Override
            public Object get() throws RemotingException {
                try {
                    return FutureAdapter.this.get();
                } catch (InterruptedException e) {
                    throw new RemotingException(e);
                } catch (ExecutionException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public Object get(int timeoutInMillis) throws RemotingException {
                try {
                    return FutureAdapter.this.get(timeoutInMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RemotingException(e);
                } catch (ExecutionException e) {
                    throw new RemotingException(e);
                } catch (TimeoutException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public void setCallback(ResponseCallback callback) {
                FutureAdapter.this.setCallback(callback);
            }

            @Override
            public boolean isDone() {
                return FutureAdapter.this.isDone();
            }
        };
    }

    void setCallback(ResponseCallback callback) {
        if (!(future instanceof org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter)) {
            return;
        }
        org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter futureAdapter = (org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter) future;
        BiConsumer<AppResponse, ? super Throwable> biConsumer = new BiConsumer<AppResponse, Throwable>() {

            @Override
            public void accept(AppResponse appResponse, Throwable t) {
                if (t != null) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    callback.caught(t);
                } else {
                    if (appResponse.hasException()) {
                        callback.caught(appResponse.getException());
                    } else {
                        callback.done((V) appResponse.getValue());
                    }
                }
            }
        };
        futureAdapter.getAppResponseFuture().whenComplete(biConsumer);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public boolean isDone() {
        return future.isDone();
    }

    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
}
