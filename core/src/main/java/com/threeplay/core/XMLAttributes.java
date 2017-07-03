package com.threeplay.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by eliranbe on 5/11/16.
 */
public class XMLAttributes {
    public final int count;
    private Map<String, String> attributes = new HashMap<>();

    public XMLAttributes(Map<String, String> attributes){
        this.attributes.putAll(attributes);
        count = this.attributes.size();
    }

    public XMLAttributes(XMLAttribute[] attributes){
        for(XMLAttribute attribute: attributes){
            this.attributes.put(attribute.name, attribute.value);
        }
        count = this.attributes.size();
    }

    public String valueForName(String name){
        return attributes.get(name);
    }

    public Set<String> names(){
        return attributes.keySet();
    }

    public void copyAttributes(Map<String, String> map){
        map.putAll(attributes);
    }

    public XMLAttributes mergeWithAttributes(XMLAttributes attributes){
        final Map<String, String> result = new HashMap<>();
        copyAttributes(result);
        attributes.copyAttributes(result);
        return new XMLAttributes(result);
    }

    public Map<String, String> map(){
        final Map<String, String> copy = new HashMap<>();
        copy.putAll(attributes);
        return copy;
    }


    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        for(Map.Entry<String, String> entry: attributes.entrySet()){
            result.append(entry.getKey());
            result.append("=\"");
            result.append(entry.getValue());
            result.append("\" ");
        }
        return result.toString();
    }

    static class XMLAttribute {
        public final String name;
        public final String value;
        public XMLAttribute(String name, String value){
            this.name = name;
            this.value = value;
        }
    }
}
