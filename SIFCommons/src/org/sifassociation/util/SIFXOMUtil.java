/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.XPathContext;
import org.xml.sax.XMLReader;

/**
 * Class contains tools to traverse a XOM tree and take common actions.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class SIFXOMUtil {
    
    /* Output */
    
    // So we can pretty print.
    public static String pretty(Element oldRoot) {    
        Element root = new Element(oldRoot);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer = new Serializer(out);
        serializer.setIndent(2);
            try {
                serializer.write(new Document(root));
            } catch (IOException ex) {
                return root.toXML();
            }

        String output;
            try {
                output = out.toString("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                return root.toXML();
            }

        // So we drop the XML version and encoding declarations.
        return output.substring(output.indexOf(serializer.getLineSeparator()) + 1);        
    }
    
    /* Edit */
    
    /**
     * Strips remove from the front of the root's and its children's names.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param remove  The string to strip from the left hand side.
     * @param limits The elements whose children will be ignored.
     */
    public static void lStrip(Element root, String remove, 
            List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each element is modified.
                    String local = node.getLocalName();
                    if(local.startsWith(indicator)) {
                        node.setLocalName(local.substring(indicator.length()));
                    }
                }
            }
            , remove, limits);
    }
    
    /**
     * Prepends add to the front of the root's and its children's names.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param remove  The string to prepend.
     * @param limits The elements whose children will be ignored.
     */
    public static void prepend(Element root, final String add, 
            List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each element is modified.
                    String local = node.getLocalName();
                    node.setLocalName(add.concat(local));
                }
            }
            , add, limits);
    }
     
    /**
     * Replaces the namespace root and its children are in.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param namespace  The namespace all the modified nodes will belong to.
     * @param limits The elements whose children will be ignored.
     */
    public static void renamespace(Element root, String namespace, 
            List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each element is modified.
                    node.setNamespaceURI(indicator);
                }
            }
            , namespace, limits);
    }

    // So we can quickly and consistently edit all matching values.
    // Pass in a null value to remove identified element.
    public static void editValue(Element root, final String tagName, 
            final String namespace, final String value, List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each matching element is modified.
                    if(0 == tagName.compareTo(node.getLocalName()) &&
                            0 == namespace.compareTo(node.getNamespaceURI())) {
                        // So we remove unwanted elements.
                        if(null == value) {
                            node.detach();
                        }
                        else {
                            node.removeChildren();
                            node.appendChild(value);
                        }
                    }
                }
            }
            , value, limits);        
    }
    
    // So we can quickly and consistently edit all matching attribute values.
    public static void editAttribute(Element root, final String tagName, 
            final String namespace, final String attributeName, 
            final String value, List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each matching element is modified.
                    if(0 == tagName.compareTo(node.getLocalName()) &&
                            0 == namespace.compareTo(node.getNamespaceURI())) {
                        Attribute edit = node.getAttribute(attributeName);
                        if(null != edit) {
                            edit.setValue(value);
                        }
                    }
                }
            }
            , value, limits);        
    }
    
    /**
     * Visits all elements in the (sub)tree indicated by root and takes action.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param visit  What action to take at each element.
     * @param indicator The modification to be made.
     * @param limits The elements whose children will be ignored.
     */
    private static void traverse(Element root, IElementAction visit, 
            String indicator, List<Element> limits) {
        // So we start at the beginning.
        Element current = root;
        
        // So we modify every node.
        List<Element> children = new ArrayList<Element>();
        int i = 0;
        
        // So we visit every element.
        while(null != current) {
            if(! contains(limits, current)) {
                combine(children, current.getChildElements());
            }
            
            visit.action(current, indicator);
                        
            current = (children.size() > i) ? (Element)children.get(i) : null;
            i++;
        }
    }
    
    /* Retrieve via XPath */
    
    // Returns the value of the first matching XPath or null.
    // Note: To use the ns namespace use the prefix "ns" in your xpath.
    // Example: /ns:environment/ns:consumerName
    public static String getFirstXPath(String xpath, Element root, String ns) {
        // So we use the specified namespace.
        XPathContext xpc = new XPathContext();
        xpc.addNamespace("ns", ns);

        // So we execute the query.
        Nodes results = root.copy().query(xpath, xpc);
        if(0 < results.size()) {
            Node first = results.get(0);
            if(null != first) {
                return first.getValue();
            }
        }
        
        // So we know the difference between failure and a blank field.
        return null;
    }


    /**
     * Returns the value of all matching XPath or an empty list.
     * Note: To use the ns namespace use the prefix "ns" in your xpath.
     * Example: /ns:environment/ns:consumerName
     * 
     * @param xpath
     * @param root
     * @param ns
     * @return
     * @see addGenericNamespace
     * @since 3.2
     */
    public static List<String> getAllXPath(String xpath, Element root, String ns) {
        // So we can return the results.
        List<String> matches = new ArrayList();

        // So we use the specified namespace.
        XPathContext xpc = new XPathContext();
        xpc.addNamespace("ns", ns);

        // So we execute the query.        
        Nodes results = root.copy().query(xpath, xpc);
        for(int i = 0; i < results.size(); i++) {
            Node current = results.get(i);
            if(null != current) {
                matches.add(current.getValue());
            }
        }
        
        // So we know the difference between failure and a blank field.
        return matches;
    }
    
    /**
     * Convert a non-qualified XPath with one qualified with the "ns" prefix.
     * 
     * @param xpath  XPath to base the namespace qualified one on.
     * @return 
     * @since 3.2
     */
    public static String addGenericNamespace(String xpath) {
        StringBuilder qualified = new StringBuilder();
        String[] parts = xpath.split("/");
        for(String part : parts) {
            if(!part.isEmpty()) {
                if(part.startsWith("@") || part.contains(":")) {
                    qualified.append("/");
                }
                else {
                    qualified.append("/ns:");
                }
                qualified.append(part);
            }
        }
        return qualified.toString();
    }
    
    /* Supporting Partial Updates */
    
    // So we can quickly and consistently find all the data XPaths
    public static Set<String> getXPaths(Element root) {
        List<String> parents = new ArrayList<String>();;
        Set<String> xpaths = new LinkedHashSet<String>();
        
        buildXPath(root, parents, xpaths);
        
        return xpaths;
    }

    // So we follow each child in the XML tree.
    private static void buildXPath(Element next, List<String> parents, 
            Set <String> xpaths) {
        // HEAD
        
        Elements children = next.getChildElements();
        
        // So we remember.
        parents.add(next.getQualifiedName());
        
        // So we keep what we went looking for.
        if(0 == children.size()) {
            xpaths.add(toXPath(parents, ""));
        }
        for(int i = 0; i < next.getAttributeCount(); i++) {
            xpaths.add(toXPath(parents, "@" + next.getAttribute(i).getQualifiedName()));
        }
        
        // RECURSE
        for(int i = 0; i < children.size(); i++) {
            buildXPath(children.get(i), parents, xpaths);
        }
        
        // TAIL
        
        // So we forget.
        parents.remove(parents.size()-1);
    }

    // So we can quickly and consistently find all the high level lists.
    public static Set<String> getListXPaths(Element root) {
        List<String> parents = new ArrayList<String>();
        Set<String> xpaths = new LinkedHashSet<String>();
        
        buildListXPath(root, parents, xpaths);
        
        return xpaths;
    }

    // So we find each high level list in the XML tree.
    private static void buildListXPath(Element next, List<String> parents, 
            Set <String> xpaths) {
        // HEAD
        
        Elements children = next.getChildElements();
        
        // So we remember.
        parents.add(next.getQualifiedName());
        
        // So we keep what we went looking for.
        Element last = null;
        Element current = null;
        for(int i = 0; i < children.size(); i++) {
            current = children.get(i);
            if(null != last) {
                if(similar(current, last)) {
                    xpaths.add(toXPath(parents, ""));
                    parents.remove(parents.size()-1);
                    return;
                }
            }
            last = current;
        }
        
        // RECURSE
        for(int i = 0; i < children.size(); i++) {
            buildListXPath(children.get(i), parents, xpaths);
        }
        
        // TAIL
        
        // So we forget.
        parents.remove(parents.size()-1);
    }
    
    // So we can quickly and consistently combine the pieces of an XPath.
    private static String toXPath(List<String> parents, String child) {
        String combined = "";
        for(String parent : parents) {
            combined = combined + "/" + parent;
        }
        if(!child.isEmpty()) {
            combined = combined + "/" + child;
        }
        return combined;
    }
    
    // So we know if we are updating a field or replacing a list.
    // Returns the highest level list or the empty string if a field.
    public static String startsWithList(String xpath, Set<String> lists) {
        for(String list : lists) {
            if(xpath.startsWith(list)) {
                return list;
            }
        }
        return "";
    }
    
    /* Supporting query-by-example */

    /**
     * Takes the root of an XML example and turns it into an XQuery.
     * 
     * @param example
     * @return 
     * @since 3.2
     */
    public static String example2XQuery(Element example) {
        // So we can build up our XQuery easily and efficently.
        StringBuilder xquery = new StringBuilder();        
        
        // So we handle the default namespace.
        xquery.append("declare default element namespace ");
        String ns = example.getNamespaceURI();
        xquery.append(ns);
        xquery.append(";\n");
        
        // So we have the XPath.
        xquery.append(example2XPath(example));
        
        return xquery.toString();
    }
    
    /**
     * Takes the root of an XML example and turns it into an XPath.
     * 
     * @param example
     * @return 
     * @since 3.2
     */
    public static String example2XPath(Element example) {
        // So we can build up our XPath easily and efficently.
        StringBuilder xpath = new StringBuilder();
        
        // So we retrieve the target object(s).
        xpath.append("/");
        xpath.append(example.getLocalName());
        
        // So we can easily remove the target object from its qualifiers.
        String start = "/" + example.getLocalName() + "/";
        
        // So we match the data in the example.
        Set<String> paths = null;
        paths = SIFXOMUtil.getXPaths(example);
        if(!paths.isEmpty()) {
            // So we deliminate the qualifiers.
            xpath.append("[");
            
            // So we have all the qualifiers.
            StringBuilder qualifiers = new StringBuilder();
            for(String path : paths) {
                List<String> values = getAllXPath(
                        addGenericNamespace(path), 
                        example, 
                        example.getNamespaceURI());
                for(String value : values) {
                    if(0 != qualifiers.length()) {
                        qualifiers.append(" and ");
                    }
                    qualifiers.append(path.replaceFirst(start, ""));
                    qualifiers.append("=");
                    qualifiers.append("\"");
                    qualifiers.append(value);
                    qualifiers.append("\"");
                }
            }
            xpath.append(qualifiers);
            
            xpath.append("]");
        }
        
        return xpath.toString();
    }
    
    /* Support SIF_Query */
    
    /**
     * Turns a SIF_Query into an XQuery.
     * 
     * @param query
     * @return 
     */
    public static String query2XQuery(Element query) {
        // So we can build up our XQuery easily and efficently.
        StringBuilder xquery = new StringBuilder();
        
        // So we handle the default namespace.
        xquery.append("declare default element namespace ");
        String ns = query.getNamespaceURI();
        xquery.append(ns);
        xquery.append(";\n");
        
        // So we are ready for the object or other simple XPath.
        xquery.append("for $object in ");
        
        // So we know how to group all the conditions or what example to use.
        Element conditionGroup = query.getFirstChildElement("SIF_ConditionGroup", ns);
        Element exampleWrapper = query.getFirstChildElement("SIF_Example", ns);
        if(null != conditionGroup) {
            // So we target the correct object.
            String objectName = getFirstXPath(
                    addGenericNamespace("/SIF_Query/SIF_QueryObject/@ObjectName"), query, ns);
            xquery.append("/");
            xquery.append(objectName);
            xquery.append("\n");
            
            conditionGroup2Where(conditionGroup, ns, xquery);
        }
        else if(null != exampleWrapper) {
            Element example = (Element) exampleWrapper.getChild(0);
            String xpath = example2XPath(example);
            xquery.append(xpath);
            xquery.append("\n");
        }
        
        // So we return the matching objects.
        // To Do:  Add support for SIF_QueryObject/SIF_Element!!!
        xquery.append("return $object");
        
        return xquery.toString();
    }

    /**
     * Adds a where clause to the current end of the xquery based on the SIF 2
     * condition group.
     * 
     * @param conditionGroup
     * @param ns
     * @param xquery 
     */
    private static void conditionGroup2Where(Element conditionGroup, String ns, StringBuilder xquery) {
        // So we get the outer most (limited to one) logical opperator correct.
        String outerLogic = conditionGroup.getAttributeValue("Type");
        if(null != outerLogic) {
            xquery.append("where ");
            // So we open the outer logical block.
            if(0 != "None".compareToIgnoreCase(outerLogic)) {
                xquery.append("(");
            }
            Elements conditions = conditionGroup.getChildElements("SIF_Conditions", ns);
            for(int i = 0; i < conditions.size(); i++) {
                // So we honor the outer logic.
                if(0 < i) {
                    logicalHelper(xquery, outerLogic);
                }
                // So we honor the inner (may be multiple) logical opperator.
                Element currentConditions = conditions.get(i);
                String innerLogic = currentConditions.getAttributeValue("Type");
                if(null != innerLogic) {
                    // So we open this logical block.
                    if(0 != "None".compareToIgnoreCase(innerLogic)) {
                        xquery.append("(");
                    }
                    // So we include the actual condition (may be more than one).
                    Elements condition = currentConditions.getChildElements(
                            "SIF_Condition", ns);
                    for(int j = 0; j < condition.size(); j++) {
                        if(0 < j) {
                            logicalHelper(xquery, innerLogic);
                        }
                        Element current = condition.get(j);
                        Element element = current.getFirstChildElement("SIF_Element", ns);
                        List<String> crumbs = new ArrayList<>();
                        // So we have the join wrapper and object name for SIF_ExtendedQuery.
                        String objectName = "";
                        if(null != element) {
                            objectName = element.getAttributeValue("ObjectName");
                            if(null != objectName && !objectName.isEmpty()) {
                                crumbs.add("$RESULT");
                                crumbs.add(objectName);
                            }
                            crumbs.add(element.getValue());
                        }
                        Element operator = current.getFirstChildElement("SIF_Operator", ns);
                        Element value = current.getFirstChildElement("SIF_Value", ns);
                        if(null != element && null != operator && null != value) {
                            xquery.append(crumbs2XPath(crumbs));
                            xquery.append(" ");
                            xquery.append(operatorHelper(operator.getValue()));
                            xquery.append(" ");
                            xquery.append(value.getValue());
                        }
                    }
                    // So we close this logical block.
                    if(0 != "None".compareToIgnoreCase(outerLogic)) {
                        xquery.append(")");
                    }                        
                }
                // So we close the outer logical block.
                if(0 != "None".compareToIgnoreCase(innerLogic)) {
                    xquery.append(")");
                }
                xquery.append("\n");
            }
        }        
    }
    
    /**
     * Creates an XQuery that retrieves the targeted objects as joined. 
     * Note: Results from query will need to be massaged into 
     * SIF_ExtendedQueryResults.
     * 
     * @param eQuery
     * @return 
     */
    public static String extendedQuery2XQuery(Element eQuery) {
        // So we can build up our XQuery easily and efficently.
        StringBuilder xquery = new StringBuilder();
        // So we handle the default namespace.
        xquery.append("declare default element namespace \"");
        String ns = eQuery.getNamespaceURI();
        xquery.append(ns);
        xquery.append("\";\n");        
        // So we can always adjust the joined results (order by)
        xquery.append("for $RESULT in (\n");
        // So we know all the objects in play by name.
        Set<String> objects;  // To Do: Consider inlcuding //@ObjectName instead, so we can support joins on unreturned objects.
        objects = new LinkedHashSet<>();
        Element select = eQuery.getFirstChildElement("SIF_Select", ns);
        Elements selectElements = select.getChildElements();
        for(int i = 0; i < selectElements.size(); i++) {
            Element current = selectElements.get(i);
            objects.add(current.getAttributeValue("ObjectName"));
        }
        // So we return all the objects in play.
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        Map<String, String> objectVariables = new HashMap<>();
        // So we use the same variable for the same object throughout.
        for(int i = 0; i < objects.size(); i++) {
            char variable = alphabet[i];
            String object = (String) objects.toArray()[i];
            objectVariables.put(object, String.valueOf(variable));
        }
        // So we honor joins.
        Element from = eQuery.getFirstChildElement("SIF_From", ns);
        // With out join.
        if(0 == from.getChildCount()) {
            StringBuilder woFor = new StringBuilder();
            StringBuilder woReturn = new StringBuilder();
            String objectName = from.getAttributeValue("ObjectName");
            String woVariable = objectVariables.get(objectName);
            woFor.append("    for $");
            woFor.append(woVariable);
            woFor.append(" in /");
            woFor.append(objectName);
            woFor.append("\n");
            woReturn.append("    return <JOINED>{$");
            woReturn.append(woVariable);
            woReturn.append("}</JOINED>\n");
            xquery.append(woFor);
            xquery.append(woReturn);
        }
        else {
            Elements joins = from.getChildElements("SIF_Join", ns);
            int aI = 0;  // Alphabet index.
            for(int i = 0; i < joins.size(); i++) {
                Element join = joins.get(i);
                String joinType = join.getAttributeValue("Type");
                Elements joinOns = join.getChildElements("SIF_JoinOn", ns);
                for(int j = 0; j < joinOns.size(); j++) {
                    // So we can build up the various parts seperately.
                    // Inner
                    StringBuilder iFor = new StringBuilder();
                    StringBuilder iLet = new StringBuilder();
                    StringBuilder iWhere = new StringBuilder();
                    StringBuilder iReturn = new StringBuilder();
                    // Left
                    StringBuilder lFor = new StringBuilder();
                    StringBuilder lWhere = new StringBuilder();
                    StringBuilder lReturn = new StringBuilder();
                    // Right
                    StringBuilder rFor = new StringBuilder();
                    StringBuilder rWhere = new StringBuilder();
                    StringBuilder rReturn = new StringBuilder();                  
                    Element joinOn = joinOns.get(j);
                    // Inner Join (all XQuery joins include this).
                    // So we can add each object returned by the inner join seperately.
                    iReturn.append("    return <JOINED>");
                    // Left ID
                    Element left = joinOn.getFirstChildElement("SIF_LeftElement", ns);
                    String leftId = "$" + alphabet[aI] + "ID";
                    aI++;
                    String leftName = left.getAttributeValue("ObjectName");
                    String leftVariable = objectVariables.get(leftName);
                    iFor.append("    for $");
                    iFor.append(leftVariable);
                    iFor.append(" in /");
                    iFor.append(leftName);
                    iFor.append("\n");                    
                    iLet.append("    let ");
                    iLet.append(leftId);
                    iLet.append(" := $");
                    iLet.append(leftVariable);
                    iLet.append("/");
                    iLet.append(left.getValue());
                    iLet.append("\n");
                    iReturn.append("{$");
                    iReturn.append(leftVariable);
                    iReturn.append("}");                    
                    // Right ID
                    Element right = joinOn.getFirstChildElement("SIF_RightElement", ns);
                    String rightId = "$" + alphabet[aI] + "ID";
                    aI++;
                    String rightName = right.getAttributeValue("ObjectName");
                    String rightVariable = objectVariables.get(rightName);
                    iFor.append("    for $");
                    iFor.append(rightVariable);
                    iFor.append(" in /");
                    iFor.append(rightName);
                    iFor.append("\n");                    
                    iLet.append("    let ");
                    iLet.append(rightId);
                    iLet.append(" := $");
                    iLet.append(rightVariable);
                    iLet.append("/");
                    iLet.append(right.getValue());
                    iLet.append("\n");
                    iReturn.append("{$");
                    iReturn.append(rightVariable);
                    iReturn.append("}");                    
                    // So we actually inner join.
                    iWhere.append("    where ");
                    iWhere.append(leftId);
                    iWhere.append("=");
                    iWhere.append(rightId);
                    iWhere.append("\n");
                    // So we close the inner join.
                    iReturn.append("</JOINED>");                                        
                    // Left Join (in addition to the data from the Inner Join).               
                    if(0 == joinType.compareTo("LeftOuter") || 
                            0 == joinType.compareTo("FullOuter")) {
                        lFor.append("    for $");
                        lFor.append(leftVariable);
                        lFor.append(" in /");
                        lFor.append(leftName);
                        lFor.append("\n");
                        lWhere.append("    where fn:empty(/");
                        lWhere.append(rightName);
                        lWhere.append("[");
                        lWhere.append(right.getValue());
                        lWhere.append(" = $");
                        lWhere.append(leftVariable);
                        lWhere.append("/");
                        lWhere.append(left.getValue());
                        lWhere.append("])\n");
                        lReturn.append("    return <JOINED>{$");
                        lReturn.append(leftVariable);
                        lReturn.append("}</JOINED>");                    
                    }
                    // Right Join (in addition to the data from the Inner Join).               
                    if(0 == joinType.compareTo("RightOuter") || 
                            0 == joinType.compareTo("FullOuter")) {
                        rFor.append("    for $");
                        rFor.append(rightVariable);
                        rFor.append(" in /");
                        rFor.append(rightName);
                        rFor.append("\n");
                        rWhere.append("    where fn:empty(/");
                        rWhere.append(leftName);
                        rWhere.append("[");
                        rWhere.append(left.getValue());
                        rWhere.append(" = $");
                        rWhere.append(rightVariable);
                        rWhere.append("/");
                        rWhere.append(right.getValue());
                        rWhere.append("])\n");
                        rReturn.append("    return <JOINED>{$");
                        rReturn.append(rightVariable);
                        rReturn.append("}</JOINED>");
                    }
                    // So we start a new FLWOR expression.
                    if(0 != joinType.compareTo("Inner") ) {
                        iReturn.append(",\n");
                    }
                    if(0 == joinType.compareTo("FullOuter") ) {
                        lReturn.append(",\n");
                    }                
                    // So we pull the data portion of the query together.
                    // Inner
                    xquery.append(iFor);
                    xquery.append(iLet);
                    xquery.append(iWhere);
                    xquery.append(iReturn);
                    // Left
                    xquery.append(lFor);
                    xquery.append(lWhere);
                    xquery.append(lReturn);
                    // Right
                    xquery.append(rFor);
                    xquery.append(rWhere);
                    xquery.append(rReturn);
                }
                // So we start a new FLWOR expression before the next join.
                if(i < joins.size()-1) {
                    xquery.append(",");
                }
                xquery.append("\n");
            }
        }
        // So we can always adjust the joined results (order by)
        xquery.append(")\n");
        // So we honor conditions (like in SIF_Query).
        Element where = eQuery.getFirstChildElement("SIF_Where", ns);
        if(null != where) {
            Element conditionGroup = where.getFirstChildElement("SIF_ConditionGroup", ns);
            conditionGroup2Where(conditionGroup, ns, xquery);
        }
        // To Do:  order by
        // So we can always adjust the joined results (order by)
        xquery.append("return $RESULT");        
        return xquery.toString();
    }
    
    /**
     * So we can consistently and efficiently handle "and" and "or."
     * 
     * @param sb  Changed in place.
     * @param logic 
     */
    private static void logicalHelper(StringBuilder sb, String logic) {
        if(0 == "Or".compareToIgnoreCase(logic)) {
            sb.append(" or ");
        }
        else {
            sb.append(" and ");
        }        
    }
    
    /**
     * Converts the SIF operator to an XQuery operator.
     * 
     * @param operator
     * @return 
     */
    private static String operatorHelper(String operator) {
        switch (operator) {
            case "LT": return "<";
            case "GT": return ">";
            case "LE": return "<=";
            case "GE": return ">=";
            case "NE": return "!=";
        }
        // EQ & the default.
        return "=";
    }
    
    /**
     * Filter's "root" until it only contains elements and attributes in "xpaths".
     * Note:  Maintains document order.
     * Note:  Keeps anything that matches regardless of namespace.
     * 
     * @param root  Changed in place.
     * @param xpaths
     * @return 
     */
    public static void greenList(Element root, List<String> xpaths) {
        // To Do: So we know we have been given reasonable parameters.

        // So we have crumbs.
        List<String> crumbs = new ArrayList();
        // So we have the first crumb.
        crumbs.add(root.getLocalName());
        // So we do something.
        greenListHelper(root, xpaths, root, crumbs);
    }
    
    private static void greenListHelper(Element root, List<String> xpaths, Element current, List<String> crumbs) {
        // So we check for unwanted attributes ever step of the way.
        for(int i = 0; i < current.getAttributeCount(); i++) {
            Attribute attribute = current.getAttribute(i);
            crumbs.add("@" + attribute.getLocalName());
            if(!xpaths.contains(crumbs2XPath(crumbs))) {
                current.removeAttribute(attribute);
            }
            crumbs.remove(crumbs.size()-1);
        }
        // Recurse:  So we visit every node in "root."
        Elements children = current.getChildElements();
        for(int i = children.size()-1; i >=0 ; i--) {  //  So we always remove the last leaf first.  Note: The usual for loop breaks badly.
            Element child = (Element) current.getChild(i);
            if(null != child) {
                crumbs.add(child.getLocalName());
                greenListHelper(root, xpaths, child, crumbs);
                crumbs.remove(crumbs.size()-1);
            }
        }
        // So we remove any leaves not present in "xpaths."
        children = current.getChildElements();
        if(0 == children.size()) {
            // So we ownly get rid of nodes with no reference.
            boolean remove = true;
            String crumbXPath = crumbs2XPath(crumbs);
            for(String xpath : xpaths) {
                if(xpath.startsWith(crumbXPath)) {
                    remove = false;
                    break;
                }
            }
            if(remove) {
                current.detach();
            }
        }              
    }
    
    private static String crumbs2XPath(List<String> crumbs)  {
        StringBuilder xpath = new StringBuilder();
        for(String current : crumbs) {
            xpath.append('/');
            xpath.append(current);
        }
        return xpath.toString();
    }
    
    /* SIFisms */
    
    /**
     * So we can support all know ID attributes consistently.
     * 
     * @param object  Root of the element we want an ID for.
     * @return The retrieved id or the empty string.
     */
    public static String getObjectId(Element object) {
        String current = null;
        current = object.getAttributeValue("refId");
        if(null != current) {
           return current;
        }
        current = object.getAttributeValue("id");
        if(null != current) {
           return current;
        }
        current = object.getAttributeValue("RefId");
        if(null != current) {
           return current;
        }
        
        return "";
    }
    
    public static Attribute createObjectId(String namespace, String value) {
        String key = "RefId";
        if(namespace.startsWith("http://www.sifassociation.org/datamodel/na/3.")) {
            key = "refId";
        }
        else if(namespace.startsWith("http://www.sifassociation.org/infrastructure/3.")) {
            key = "id";
        }
        
        return new Attribute(key, value);
    }
    
    /* Helpers */
    
    /**
     * So instead of working with Elements we can work with standard Java Lists.
     * 
     * @param existing  The list of elements that already exists (this grows).
     * @param append  The elements to add to the list.
     * @since 3.0
     */
    public static void combine(List<Element> existing, Elements append) {
        for(int i = 0; append.size() > i; i++) {
            existing.add(append.get(i));
        }
    }

    /**
     * So we can compare similar elements.
     * 
     * Note: Only the tag's namespace and name get compared.
     * 
     * @param x  One element to compare.
     * @param y  One element to compare.
     * @return similar: true  different: false
     * @since 3.0
     */
    private static boolean similar(Element x, Element y) {
        return (x.getNamespaceURI() + ":" + x.getLocalName()).equalsIgnoreCase(
                y.getNamespaceURI() + ":" + y.getLocalName());
    }
    
    /**
     * So we can look for similar elements.
     * 
     * @param haystack  The List of elements to inspect.
     * @param needle  The element to find a match for.
     * @return  combined: true  not combined: false 
     * @since 3.0
     */
    private static boolean contains(List<Element> haystack, Element needle) {
        
        for(int i = 0; i < haystack.size(); i++) {
            if(similar(haystack.get(i), needle)) {
                return true;
            }
        }
        
        return false;
    }

    // So we can simply validate a single XML file against a single XSD file.
    public static boolean validate(String xml, String xsdURL) {
        return validate(xml, xsdURL, new StringBuilder());
    }
    
    // So we can simply validate a single XML file against a single XSD file.
    public static boolean validate(String xml, String xsdURL, 
            StringBuilder details) {
        XSDErrorHandler errors = new XSDErrorHandler();
        
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            SchemaFactory schemaFactory = 
                SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            factory.setSchema(schemaFactory.newSchema(
                new Source[] {new StreamSource(xsdURL)}));
            
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();

            reader.setErrorHandler(errors);

            Builder builder = new Builder(reader);
            builder.build(xml, null);
        } catch (Exception ex) {
            Logger.getLogger(SIFXOMUtil.class.getName()).log(Level.SEVERE, null, ex);
            details.append("<p>" +  ex.getLocalizedMessage() + "</p>");
            return false;
        }

        for(String error : errors.errors) {
            details.append("<p>" + error + "</p>");
        }
        
        return !errors.hasError();
    }

}
