package name.eraxillan.financial_advisor;

import android.support.v4.util.Pair;
import android.util.Log;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class CategoryXmlParser {
    final private String MY_TAG = "SBRF Financial Advisor";

    Document getDomElement(String xml) {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);
        } catch (ParserConfigurationException e) {
            return null;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return doc;
    }

    static String getCharacterDataFromElement(Element e)
    {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData)
        {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "";
    }

    CategoryFilterType stringToCategoryFilterType(String type) {
        CategoryFilterType result = CategoryFilterType.INVALID;

        if (type.equals("startsWith")) result = CategoryFilterType.STARTS_WITH;
        else if (type.equals("equalsTo")) result = CategoryFilterType.EQUALS;

        return result;
    }

    TreeMap<String,String> xmlParseNames(Element el, String tag) {
        TreeMap<String, String> result = new TreeMap<>();

        NodeList categoryNameNodes = el.getElementsByTagName(tag);
        for (int j = 0; j < categoryNameNodes.getLength(); ++j) {
            Node categoryNameNode = categoryNameNodes.item(j);
            if (categoryNameNode.getNodeType() != Node.ELEMENT_NODE) continue;

            Element categoryNameElement = (Element)categoryNameNode;
            String nameLanguage = categoryNameElement.getAttribute("language");
            String name = getCharacterDataFromElement(categoryNameElement);

            result.put(nameLanguage, name);
        }

        return result;
    }

    private ArrayList<Pair<CategoryFilterType, String>> xmlParseSmsTargets(Element el) {
        ArrayList<Pair<CategoryFilterType, String>> result = new ArrayList<>();

        NodeList categoryNameNodes = el.getElementsByTagName("CategoryItemSmsTarget");
        for (int j = 0; j < categoryNameNodes.getLength(); ++j) {
            Node categoryNameNode = categoryNameNodes.item(j);
            if (categoryNameNode.getNodeType() != Node.ELEMENT_NODE) continue;

            Element categoryNameElement = (Element)categoryNameNode;
            String filterType = categoryNameElement.getAttribute("type");
            String filterValue = getCharacterDataFromElement(categoryNameElement);

            CategoryFilterType ft = stringToCategoryFilterType(filterType);
            result.add(new Pair<>(ft, filterValue));
        }

        return result;
    }

    public ArrayList<PaymentCategory> loadCategoriesFromFile(String fileName) {
        ArrayList<PaymentCategory> result = new ArrayList<>();

        File sharedDocsDir = EnvUtils.getSharedDocumentsDirectory();
        if (sharedDocsDir == null) return null;

        try {
            // Open the file stream
            File file = new File(sharedDocsDir.getAbsolutePath(), fileName);
            InputStream is = new FileInputStream(file);

            // Read the entire stream to the string using buffering
            final int bufferSize = 1024;
            final char[] buffer = new char[bufferSize];
            final StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(is, "UTF-8");
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0) break;
                out.append(buffer, 0, rsz);
            }
            String xmlContents = out.toString();

            // Parse XML
            Document categoryXmlDoc = getDomElement(xmlContents);
            NodeList categoryXmlNodes = categoryXmlDoc.getElementsByTagName("Category");
            for (int i = 0; i < categoryXmlNodes.getLength(); ++i) {
                PaymentCategory category = new PaymentCategory();

                Node categoryXmlNode = categoryXmlNodes.item(i);
                if (categoryXmlNode.getNodeType() != Node.ELEMENT_NODE) continue;
                Element categoryXmlElement = (Element)categoryXmlNode;

                // Category names
                TreeMap<String, String> categoryNames = xmlParseNames(categoryXmlElement, "CategoryName");
                for (String lng : categoryNames.keySet()) {
                    category.addName(lng, categoryNames.get(lng));
                }

                // Category items
                NodeList categoryItemNodes = categoryXmlElement.getElementsByTagName("CategoryItem");
                for (int j = 0; j < categoryItemNodes.getLength(); ++j) {
                    PaymentCategoryItem categoryItem = new PaymentCategoryItem();

                    Node categoryItemNode = categoryItemNodes.item(j);
                    if (categoryItemNode.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element categoryItemXmlElement = (Element)categoryItemNode;

                    // Category item name
                    TreeMap<String, String> categoryItemNames = xmlParseNames(categoryItemXmlElement, "CategoryItemName");
                    for (String lng : categoryItemNames.keySet()) {
                        categoryItem.addName(lng, categoryItemNames.get(lng));
                    }

                    // Category item SMS target search pattern
                    ArrayList<Pair<CategoryFilterType, String>> categoryItemFilters = xmlParseSmsTargets(categoryItemXmlElement);
                    for (Pair<CategoryFilterType, String> cf : categoryItemFilters) {
                        categoryItem.addFilter(cf);
                    }

                    category.addItem(categoryItem);
                }

                result.add(category);
            }
        }
        catch (Exception exc) {
            Log.e(MY_TAG, "Unable to load or parse XML file with categories: " + exc.getLocalizedMessage());
            return null;
        }

        return result;
    }
}
