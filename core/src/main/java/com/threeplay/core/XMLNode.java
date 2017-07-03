package com.threeplay.core;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

/**
 * Created by eliranbe on 5/5/16.
 */
public class XMLNode {
    public final String tag;
    public final XMLAttributes attributes;
    public final String text;
    public final XMLNodes nodes;

    public static XMLNodeBuilder builder(String tag){
        return new XMLNodeBuilder(tag);
    }

    public XMLNode(String tag, XMLAttributes attributes, XMLNodes nodes, String text) {
        this.tag = tag;
        this.attributes = attributes != null ? attributes : new XMLAttributes(new HashMap<String, String>());
        this.nodes = nodes != null ? nodes : new XMLNodes(new LinkedList<XMLNode>());
        this.text = text;
    }

    public XMLNode nodeWithTag(String tag){
        for (XMLNode node: nodes) {
            if ( node.tag.equals(tag) ) {
              return node;
            }
        }
        return null;
    }

    public XMLNode nodeAtIndex(int index) {
        return nodes.get(index);
    }

    public int nodesCount() {
        return nodes.count;
    }

    public XMLNode get(int index) {
        return nodes.get(index);
    }

    public int attributesCount() {
        return attributes.count;
    }

    public Set<String> attributeNames() {
        return attributes.names();
    }

    public String attributeValue(String attributeName){
        return attributes.valueForName(attributeName);
    }

    public String attributeValue(String attributeName, String defaultValue){
        String value = attributeValue(attributeName);
        if ( value == null ) return defaultValue;
        return value;
    }

    public Integer attributeInteger(String attributeName){
        return attributeInteger(attributeName, 0);
    }

    public Integer attributeInteger(String attributeName, Integer defaultValue){
        String value = attributeValue(attributeName);
        if ( value == null ) return defaultValue;
        return Integer.valueOf(value);
    }

    public static XMLNode parse(String xmlString) {
        return parse(new InputSource(new StringReader(xmlString)));
    }

    public static XMLNode parse(InputStream stream){
        return parse(new InputSource(stream));
    }

    public static XMLNode parse(InputSource source){
        XMLNodeParser nodeParser = new XMLNodeParser();
        try {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", false);
            xmlReader.setContentHandler(nodeParser);
            xmlReader.setErrorHandler(nodeParser);
            xmlReader.parse(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodeParser.rootNode();
    }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("<");
        result.append(tag);
        result.append(" ");
        result.append(attributes.toString());
        result.append(nodesCount() > 0 ? "...>" : "/>");
        return result.toString();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class XMLNodeBuilder {
        private String tag;
        private Map<String, String> attributes = new HashMap<>();
        private StringBuilder text = new StringBuilder();
        private List<XMLNode> nodes = new LinkedList<>();

        public XMLNodeBuilder(String tag){
            this.tag = tag;
        }

        public XMLNodeBuilder addAttribute(String name, String value){
            attributes.put(name, value);
            return this;
        }

        public XMLNodeBuilder addAttributes(Map<String, String> attributes){
            this.attributes.putAll(attributes);
            return this;
        }

        public XMLNodeBuilder addAttributes(XMLAttributes attributes){
            attributes.copyAttributes(this.attributes);
            return this;
        }

        public XMLNodeBuilder appendText(String text){
            this.text.append(text);
            return this;
        }

        public XMLNodeBuilder addNode(XMLNode node){
            nodes.add(node);
            return this;
        }

        public XMLNodeBuilder addNodes(XMLNode... nodes){
            this.nodes.addAll(Arrays.asList(nodes));
            return this;
        }
        public XMLNodeBuilder addNodes(XMLNodes nodes){
            for (XMLNode node: nodes) {
                this.nodes.add(node);
            }
            return this;
        }

        public XMLNode build(){
            return new XMLNode(tag, new XMLAttributes(attributes), new XMLNodes(nodes), text.toString());
        }
    }

    static class XMLNodeParser extends DefaultHandler {
        private static final String TAG = "XMLNodeParser";

        private XMLNode rootNode;
        private List<XMLNodeBuilder> nodeBuilders = new LinkedList<>();
        private XMLNodeBuilder currentBuilder;

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
        }

        public XMLNode rootNode(){
            return rootNode;
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            XMLNodeBuilder builder = startElementWithTag(qName);
            int attributesCount = attributes.getLength();
            for ( int i = 0; i < attributesCount; ++i ){
                builder.addAttribute(attributes.getQName(i), attributes.getValue(i));
            }
        }

        private XMLNodeBuilder startElementWithTag(String tag){
            XMLNodeBuilder builder = new XMLNodeBuilder(tag);
            nodeBuilders.add(builder);
            return currentBuilder = builder;
        }

        private XMLNode endElement(){
            if ( currentBuilder != null ) {
                XMLNode node = currentBuilder.build();
                int count = nodeBuilders.size();
                nodeBuilders.remove(count-1);
                currentBuilder = count > 1 ? nodeBuilders.get(count-2) : null;
                return node;
            }
            return null;
        }


        @Override
        public void characters(char[] chars, int offset, int length) throws SAXException {
            currentBuilder.appendText(String.valueOf(chars, offset, length));
        }

        @Override
        public void endElement(String s, String s1, String s2) throws SAXException {
            XMLNode node = endElement();
            if ( currentBuilder != null ) {
                currentBuilder.addNode(node);
            }
            else rootNode = node;
        }

        @Override
        public void endDocument() throws SAXException {

        }

        @Override
        public void setDocumentLocator(Locator locator) {

        }
    }
}

