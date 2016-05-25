package com.github.tpeltola.rx.wikipedia;

import static com.github.tpeltola.rx.wikipedia.Schedulers.javaFxEventThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.Observable.concat;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.*;
import javafx.event.*;
import javafx.scene.Node;

import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

public class Observables {
    
    public static <E extends Event> Observable<E> fromEvent(Node source, EventType<E> type) {
        return Observable.create(subscriber -> {
            EventHandler<E> handler = event -> {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(event);
                }
            };
            source.addEventHandler(type, handler);
            
            subscriber.add(Subscriptions.create(() -> source.removeEventHandler(type, handler)));
        });
    }
    
    public static <T> Observable<T> fromProperty(ReadOnlyProperty<T> property) {
        return Observable.create(subscriber -> {
            ChangeListener<T> listener = (value, oldV, newV) -> {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(newV);
                }
            };
            property.addListener(listener);
            
            subscriber.add(Subscriptions.create(() -> property.removeListener(listener)));
        });
    }
    
    public static <T> ReadOnlyProperty<T> toProperty(Observable<T> observable) {
        ReadOnlyObjectWrapper<T> property = new ReadOnlyObjectWrapper<>();
        observable.observeOn(javaFxEventThread())
            .subscribe(value -> property.set(value));
        
        return property.getReadOnlyProperty();
    }
    
    public static <T> ObservableList<T> toObservableList(Observable<List<T>> observable) {
        ObservableList<T> list = FXCollections.observableArrayList();
        
        observable.observeOn(javaFxEventThread())
            .subscribe(l -> list.setAll(l));
        
        return list;
    }
    
    public static <T> Observable<T> fromFuture(CompletableFuture<T> f) {
        return Observable.create(subscriber -> {
            CompletableFuture<Object> handler = f.handleAsync((v, e) -> {
                if (e == null) {
                    subscriber.onNext(v);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(e);
                }
                return null;
            });
            subscriber.add(Subscriptions.create(() -> handler.cancel(false)));
        });
    }
    
    public static <T> Observable<Optional<T>> recovered(Observable<T> obs) {
        return obs.map(Optional::of).onErrorResumeNext(e -> Observable.just(Optional.<T>empty()));
    }
    
    public static <T> Observable<T> timedOut(Observable<T> obs, long seconds) {
        return obs.takeUntil(Observable.interval(seconds, SECONDS));
    }
    
    public static <T, U> Observable<Optional<U>> concatRecovered(Observable<T> obs, Func1<T, Observable<U>> request) {
        return concat(
            obs.map(request)
                .map(Observables::recovered)
        );
    }
    
}
