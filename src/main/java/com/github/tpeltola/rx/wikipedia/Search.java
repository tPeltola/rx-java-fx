package com.github.tpeltola.rx.wikipedia;

import static com.github.tpeltola.rx.wikipedia.Observables.fromFuture;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javax.xml.parsers.*;

import rx.Observable;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class Search {
    private final DocumentBuilderFactory docBuilderFactory;
    
    public Search() {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
    }
    
    public CompletableFuture<List<String>> suggestion(String term) {
        return CompletableFuture.supplyAsync(() -> doSuggest(term));
    }
    
    private List<String> doSuggest(String term) {
        Document doc = get("https://fi.wikipedia.org/w/api.php?action=opensearch&format=xml&limit=15&search=" + term);
        Element section = getElementByTagName(doc.getDocumentElement(), "Section");
        NodeList items = section.getElementsByTagName("Item");
        
        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            suggestions.add(getElementByTagName(item, "Text").getTextContent());
        }
        
        return suggestions;
    }

    private Element getElementByTagName(Element element, String name) {
        NodeList elements = element.getElementsByTagName(name);
        assert elements.getLength() == 1;
        return (Element) elements.item(0);
    }
    
    public CompletableFuture<String> page(String term) {
        return CompletableFuture.supplyAsync(() -> fetchPage(term));
    }
    
    private String fetchPage(String term) {
        Document doc = get("https://fi.wikipedia.org/w/api.php?action=parse&format=xml&prop=text&section=0&page=" + term);
        Element parse = getElementByTagName(doc.getDocumentElement(), "parse");
        return getElementByTagName(parse, "text").getTextContent();
    }
    
    public Observable<List<String>> suggestionStream(String term) {
        return fromFuture(suggestion(term));
    }
    
    public Observable<String> pageStream(String term) {
        return fromFuture(page(term));
    }

    private Document get(String resource) {
        HttpURLConnection connection = null;
        try {
            
            URL url = new URL(resource);
            connection = (HttpURLConnection) url.openConnection();
            int status = connection.getResponseCode();
            if (status != 200) {
                throw new IllegalArgumentException("Got status " + status);
            }
            Document doc = docBuilderFactory.newDocumentBuilder().parse(connection.getInputStream());
            return doc;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
