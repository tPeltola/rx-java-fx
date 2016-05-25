package com.github.tpeltola.rx.wikipedia;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.*;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.*;
import javafx.stage.Stage;

import rx.Observable;

public class Wikipedia extends Application {
    
    private final Search search = new Search();
    
    private TextField searchTerm;
    private ListView<String> suggestionList;
    private WebView editor;
    private Label status;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scene scene = new Scene(buildRoot(), 900, 600);
        
        wire();
        
        primaryStage.setTitle("Hae Wikipediasta");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Pane buildRoot() {
        VBox root = new VBox();
        root.setPadding(new Insets(5));
        root.getChildren().add(buildContent());
        status = new Label(" ");
        root.getChildren().add(status);
        return root;
    }

    private Pane buildContent() {
        HBox content = new HBox();
        content.getChildren().add(buildSuggestions());
        editor = new WebView();
        editor.setContextMenuEnabled(false);
        content.getChildren().add(editor);
        return content;
    }

    private Pane buildSuggestions() {
        VBox suggestions = new VBox();
        suggestions.setMaxSize(240, 900);
        suggestions.setPadding(new Insets(10));
        suggestions.getChildren().add(buildSearch());
        suggestionList = new ListView<>();
        suggestionList.setPrefHeight(900);
        suggestions.getChildren().add(suggestionList);
        return suggestions;
    }

    private Pane buildSearch() {
        HBox search = new HBox();
        search.setMaxSize(640, 30);
        search.setPadding(new Insets(5, 0, 5, 0));
        searchTerm = new TextField();
        search.getChildren().add(searchTerm);
        return search;
    }
    
    private void wire() {
        Observable<Optional<List<String>>> suggestionStream = Observables.concatRecovered(
            Observables.fromProperty(searchTerm.textProperty())
                .debounce(500, MILLISECONDS), 
            search::suggestionStream
        ).share();
        
        suggestionList.setItems(Observables.toObservableList(
            suggestionStream
                .filter(Optional::isPresent)
                .map(Optional::get)
        ));
        
        Observable<Optional<String>> pages = Observables.recovered(
            Observables.fromProperty(suggestionList.getSelectionModel().selectedItemProperty())
                .map(dirty -> dirty.replace(' ', '_'))
                .flatMap(search::pageStream)
        ).share();
        
        pages.observeOn(Schedulers.javaFxEventThread())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .subscribe(p -> editor.getEngine().loadContent(p));
        
        status.textProperty().bind(Observables.toProperty(
            Observable.merge(suggestionStream, pages)
                .filter(o -> !o.isPresent())
                .map(e -> "Virhe ehdotusten haussa")
        ));
    }
    
    public static void main(String[] args) {
        launch(args);
    }

}
