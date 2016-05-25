package com.github.tpeltola.rx.wikipedia;

import java.util.concurrent.*;

import javafx.application.Platform;

import rx.*;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public final class Schedulers {
    
    public static Scheduler javaFxEventThread() {
        return new JavaFxScheduler();
    }
    
    final static class JavaFxScheduler extends Scheduler {

        @Override
        public Worker createWorker() {
            return new JavaFxWorker();
        }
        
    }
    
    final static class JavaFxWorker extends Worker {
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void unsubscribe() {
            executor.shutdown();
        }

        @Override
        public boolean isUnsubscribed() {
            return executor.isShutdown();
        }

        @Override
        public Subscription schedule(Action0 action) {
            Platform.runLater(() -> action.call());
            return Subscriptions.empty();
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            ScheduledFuture<?> future = executor.schedule(() -> Platform.runLater(() -> action.call()), delayTime, unit);
            return Subscriptions.create(() -> future.cancel(false));
        }
        
    }

}
