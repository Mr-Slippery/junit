package org.junit.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.FixMethodOrder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class MethodSorter {
    /**
     * DEFAULT sort order
     */
    public static final Comparator<Method> DEFAULT = new Comparator<Method>() {
        public int compare(Method m1, Method m2) {
            int i1 = m1.getName().hashCode();
            int i2 = m2.getName().hashCode();
            if (i1 != i2) {
                return i1 < i2 ? -1 : 1;
            }
            return NAME_ASCENDING.compare(m1, m2);
        }
    };

    /**
     * Method name ascending lexicographic sort order, with {@link Method#toString()} as a tiebreaker
     */
    public static final Comparator<Method> NAME_ASCENDING = new Comparator<Method>() {
        public int compare(Method m1, Method m2) {
            final int comparison = m1.getName().compareTo(m2.getName());
            if (comparison != 0) {
                return comparison;
            }
            return m1.toString().compareTo(m2.toString());
        }
    };

    
    private static Map<String, Integer> processXML(String fileName) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser;
            try {
                saxParser = spf.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();
                xmlReader.setContentHandler(new SaxLocalNameCount());
                xmlReader.parse(convertToFileURL(fileName));
                return ((SaxLocalNameCount)xmlReader.getContentHandler()).getTags();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
    }
    
    private static String convertToFileURL(String fileName) {
        String path = new File(fileName).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    private static Comparator<Method> mapBasedComparator(final Map<String, Integer> methodMap) {
        return new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                Integer int1 = methodMap.get(o1.getName());
                Integer int2 = methodMap.get(o2.getName());
                if (int1 != null && int2 != null) {
                    return int1.compareTo(int2);
                }
                return 0;
            }            
        };
    }
    /**
     * Gets declared methods of a class in a predictable order, unless @FixMethodOrder(MethodSorters.JVM) is specified.
     *
     * Using the JVM order is unwise since the Java platform does not
     * specify any particular order, and in fact JDK 7 returns a more or less
     * random order; well-written test code would not assume any order, but some
     * does, and a predictable failure is better than a random failure on
     * certain platforms. By default, uses an unspecified but deterministic order.
     *
     * @param clazz a class
     * @return same as {@link Class#getDeclaredMethods} but sorted
     * @see <a href="http://bugs.sun.com/view_bug.do?bug_id=7023180">JDK
     *      (non-)bug #7023180</a>
     */
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        FixMethodOrder fixMethodOrder = clazz.getAnnotation(FixMethodOrder.class);
        Comparator<Method> comparator = null;
        if (fixMethodOrder != null) {
            comparator = getSorter(fixMethodOrder); 
        } else {
            String testReport = System.getProperty("testReport");
            if (testReport != null) {
                comparator = mapBasedComparator(processXML(testReport));
            }
            if (comparator == null) {
                comparator = getSorter(fixMethodOrder);             
            }
        }
         
        Method[] methods = clazz.getDeclaredMethods();
        if (comparator != null) {
            Arrays.sort(methods, comparator);
        }

        return methods;
    }

    private MethodSorter() {
    }

    private static Comparator<Method> getSorter(FixMethodOrder fixMethodOrder) {
        if (fixMethodOrder == null) {
            return DEFAULT;
        }

        return fixMethodOrder.value().getComparator();
    }
}
