package com.threeplay.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by eliranbe on 5/5/16.
 */
public class XMLNodes implements Iterable<XMLNode> {
    public final int count;
    private XMLNode[] nodes;

    public static XMLNodes.Builder builder(){
        return new XMLNodes.Builder();
    }

    public XMLNodes(XMLNode... nodes){
        this.nodes = Arrays.copyOf(nodes, nodes.length);
        this.count = this.nodes.length;
    }
    public XMLNodes(List<XMLNode> nodes){
        this(nodes.toArray(new XMLNode[nodes.size()]));
    }

    public XMLNode get(int index){
        if ( index >= 0 && index < count )
            return nodes[index];
        return null;
    }

    public List<XMLNode> get(String tagName){
        final List<XMLNode> foundNodes = new LinkedList<>();
        for (XMLNode node: nodes) {
            if ( node.tag.equals(tagName) )
                foundNodes.add(node);
        }
        return foundNodes;
    }

    public XMLNode getFirst(String tagName) {
        for (XMLNode node: nodes) {
            if ( node.tag.equals(tagName) ) return node;
        }
        return null;
    }

    public List<XMLNode> matches(String regex){
        Pattern pattern = Pattern.compile(regex);
        final List<XMLNode> foundNodes = new LinkedList<>();
        for (XMLNode node: nodes) {
            if ( pattern.matcher(node.tag).matches() )
                foundNodes.add(node);
        }
        return foundNodes;
    }

    public Iterator<XMLNode> iterator(){
        return Arrays.asList(nodes).iterator();
    }

    public void appendToList(List<XMLNode> list) {
        Collections.addAll(list, nodes);
    }

    public static class Builder {
        private final List<XMLNode> nodes = new LinkedList<>();

        public void addNodes(XMLNodes nodes){
            if ( nodes != null ) nodes.appendToList(this.nodes);
        }

        public void addNode(XMLNode node){
            if ( node != null ) this.nodes.add(node);
        }

        public XMLNodes build(){
            return new XMLNodes(nodes);
        }
    }
}
