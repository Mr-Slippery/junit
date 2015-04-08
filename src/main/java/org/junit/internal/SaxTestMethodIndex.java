package org.junit.internal;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxTestMethodIndex extends DefaultHandler {

    private String suite = "";
    private Map<String, Integer> tags;
    private int count = 0;
    
    public Map<String, Integer> getTags() {
        return tags;
    }
    
    @Override
    public void startDocument() throws SAXException {        
        tags = new HashMap<String, Integer>();
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {

        String key = localName.toLowerCase();

        if ("testsuite".equals(key)) {
            String value = atts.getValue("name");
            if (value != null) {
                suite = value;
            }
            
        }
        if ("testcase".equals(key)) {
            String value = atts.getValue("name");
            if (value != null) {
                tags.put(suite + "." + value, Integer.valueOf(count++));
            }
        }
    }
}
