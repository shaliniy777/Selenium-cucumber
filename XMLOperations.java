/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.Transform;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.experian.automation.transformers.VariablesTransformer;


/**
 * The type Xml operations.
 */
public class XMLOperations {

  private static final String DOCUMENTBUILDERFACTORY = "javax.xml.parsers.DocumentBuilderFactory";
  private static final String DOCUMENTBUILDERFACTORYIMPL = "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";

  /**
   * The method creates a new JSON file from an existing XML file.
   *
   * @param XMLFile  The full path to the XML (e.g C:/Temp/file.xml)
   * @param JSONFile The full path to the JSON file which should be created (e.g C:/Temp/file.json)
   * @throws IOException the io exception
   */
  public static void convertXMLToJSON(String XMLFile, String JSONFile) throws IOException {
    Integer OUTPUT_JSON_PRETTY_FACTOR = 1;
    File inputFile = new File(XMLFile);

    StringBuilder builder;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
      builder = new StringBuilder();

      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
    }

    JSONObject jsonObj = XML.toJSONObject(builder.toString());

    try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(JSONFile), StandardCharsets.UTF_8))) {

      bufferedWriter.write(jsonObj.toString(OUTPUT_JSON_PRETTY_FACTOR));
    }
  }

  /**
   * The method returns a json string an existing XML file with values as a strings or not.
   *
   * @param XMLFile     The full path to the XML (e.g C:/Temp/file.xml)
   * @param keepStrings the keep strings
   * @return the json object
   * @throws IOException the io exception
   * @keepStrings Option to keep the values as strings
   */
  public JSONObject convertXMLToJSON(String XMLFile, boolean keepStrings) throws IOException {
    File inputFile = new File(XMLFile);

    StringBuilder builder;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
      builder = new StringBuilder();

      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
    }

    JSONObject jsonObj = XML.toJSONObject(builder.toString(), keepStrings);
    return jsonObj;
  }

  /**
   * The method returns a json string an existing XML file.
   *
   * @param XMLFile The full path to the XML (e.g C:/Temp/file.xml)
   * @return the json object
   * @throws IOException the io exception
   */
  public JSONObject convertXMLToJSON(String XMLFile) throws IOException {
    return convertXMLToJSON(XMLFile, false);
  }

  /**
   * The method transforms an XML file to another XML based on XSLT code file
   *
   * @param outputFile The full path to the new file which will be the result of the transformation
   * @param xmlFile    The path to file that contains the data which will be transformed.
   * @param xsltFile   The path to the file which contains the XSLT code
   * @throws Exception the exception
   */
  public void XSLtransform(String outputFile, String xmlFile, String xsltFile) throws Exception {
    String[] args = {"-o:" + outputFile, xmlFile, xsltFile};
    Transform.main(args);

  }

  /**
   * Validate xml boolean.
   *
   * @param xmlString    the xml string
   * @param schemaString the schema string
   * @return the boolean
   */
  public boolean validateXML(String xmlString, String schemaString) {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    try {
      Schema schema = schemaFactory.newSchema(new StreamSource(new java.io.StringReader(schemaString)));
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

      Validator validator = schema.newValidator();
      // setProperty() calls bellow don't fix XXE issues from SonarLint locally but it is the approach mentioned here - https://rules.sonarsource.com/java/RSPEC-2755
      // And also used in a lot of places in teh powercurve project
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      validator.validate(new StreamSource(new java.io.StringReader(xmlString)));
      return true;
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Validate xml boolean.
   *
   * @param xmlString  the xml string
   * @param schemaFile the schema file
   * @return the boolean
   * @throws IOException the io exception
   */
  public boolean validateXML(String xmlString, File schemaFile) throws IOException {
    return validateXML(xmlString, FileUtils.readFileToString(schemaFile, "UTF-8"));
  }

  /**
   * Evaluate x path string.
   *
   * @param xmlString the xml string
   * @param xPath     the x path
   * @return the string
   */
  public String evaluateXPath(String xmlString, String xPath) {
    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    try {
      XPathExpression expr =
          xpath.compile(xPath);
      return (String) expr.evaluate(convertStringToDocument(xmlString), XPathConstants.STRING);
    } catch (XPathExpressionException e) {
      return "";
    }
  }

  /**
   * Remove xml namespaces string.
   *
   * @param response the response
   * @return the string
   */
  public static String removeXmlNamespaces(String response) {
    String s = response.replaceAll("xmlns([:a-zA-Z0-9]*)=(\"[^\"]+\"|\'[^\']+\')", "");
    s = s.replaceAll("(</?)[a-zA-Z0-9]+:", "$1");
    return s.replaceAll("xsi:(type|nil)=(\"[^\"]+\"|'[^']+')", "");
  }

  /**
   * Convert string to document document.
   *
   * @param xmlStr the xml str
   * @return the document
   */
  public static Document convertStringToDocument(String xmlStr) {
    System.setProperty(DOCUMENTBUILDERFACTORY, DOCUMENTBUILDERFACTORYIMPL);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    DocumentBuilder builder;
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
      return doc;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Document to string string.
   *
   * @param doc the doc
   * @return the string
   */
  public static String documentToString(Document doc) {
    return nodeToString(doc);
  }

  /**
   * Node to string string.
   *
   * @param node the node
   * @return the string
   */
  public static String nodeToString(Node node) {
    DOMSource domSource = new DOMSource(node);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    System.setProperty("javax.xml.transform.TransformerFactory","com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    TransformerFactory tf = TransformerFactory.newInstance();
    tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    try {
      tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.transform(domSource, result);
    } catch (TransformerException e) {
      e.printStackTrace();
    }
    //Add regex handling for indentation bug with CDATA between Java 9 to Java 13. More details in DIG-4876.
    return writer.toString().replaceAll(">\\s*(<\\!\\[CDATA\\[(?s).*?]]>)\\s*</", ">$1</");
  }

  /**
   * Get a value from config xml file
   * <p>
   * Example: (file, "/conf/service[@name='myservice']/@port")
   *
   * @param filePath - Path to config file
   * @param xpath    - Xpath expression
   * @return String - value
   * @throws Exception the exception
   */
  public static String getValueFromXMLFile(String filePath, String xpath) throws Exception {
    return (String) getNodesByQName(filePath, xpath, XPathConstants.STRING);
  }

  /**
   * Get NodeList from file that match the xpath expression Example: (filePath, "//input[@type=='text']")
   *
   * @param filePath - path to xml file
   * @param xpath    - Xpath expression
   * @return NodeList - itterable
   * @throws Exception the exception
   */
  public static NodeList getNodesFromXMLFile(String filePath, String xpath) throws Exception {
    return (NodeList) getNodesByQName(filePath, xpath, XPathConstants.NODESET);
  }

  private static Object getNodesByQName(String filePath, String xpath, QName type) throws Exception {
    System.setProperty(DOCUMENTBUILDERFACTORY, DOCUMENTBUILDERFACTORYIMPL);
    DocumentBuilderFactory docBuilder = DocumentBuilderFactory.newInstance();
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    docBuilder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Document doc = docBuilder.newDocumentBuilder().parse(filePath);
    XPathExpression expr = XPathFactory.newInstance().newXPath().compile(xpath);
    return expr.evaluate(doc, type);
  }

  /**
   * Reads the content of each childNode of NodeList and adds it to HashSet so that there are no duplications
   *
   * @param filePath - path to xml file
   * @param xpath    - Xpath expression
   * @return HashSet<String>  - list of unique child nodes content
   * @throws Exception the exception
   */
  public static HashSet<String> getUniqueNodes (String filePath, String xpath) throws Exception {
    NodeList nodes = getNodesFromXMLFile(filePath, xpath);
    HashSet<String> uniqueNodes = new HashSet<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      uniqueNodes.add(nodeToString(nodes.item(i)));
    }
    return uniqueNodes;
  }

  /**
   * Extract attributes from XML file.
   *
   * @param filePath  the target path to the file
   * @param xpathExpression  xpath that leads to the attribute
   * @return List<String> - list of all attribute values matching the requested xpath.
   * @throws SAXException the exception
   * @throws ParserConfigurationException the parcerConfigurationException
   * @throws XPathExpressionException the XpathExpressionException
   * @throws IOException the io exception
   */
  public static List<String> extractXMLattributes (String filePath, String xpathExpression)
      throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
    Document document = getXmlDocument(filePath);
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList nodeList = (NodeList) xPath.compile(xpathExpression).evaluate(document, XPathConstants.NODESET);

    List<String> attributes = new ArrayList<>();
    for(int i = 0; i < nodeList.getLength(); i ++) {
      attributes.add(nodeList.item(i).getTextContent());
    }
    return attributes;
  }

  /**
   * Check XML file contains attributes
   *
   * @param filePath the target path to the file.
   * @param xpathExpression xpath that leads to the attribute.
   * @return boolean
   * @throws SAXException the exception
   * @throws ParserConfigurationException the parcerConfigurationException
   * @throws XPathExpressionException the XpathExpressionException
   * @throws IOException the io exception
   */
  public static boolean containsXpathInXML(String filePath, String xpathExpression)
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    Document document = getXmlDocument(filePath);
    return containsXpathInXML(document, xpathExpression);
  }

  /**
   * Check XML file contains attributes
   *
   * @param document the xml document.
   * @param xpathExpression xpath that leads to the attribute.
   * @return boolean
   * @throws XPathExpressionException the XpathExpressionException
   */
  public static boolean containsXpathInXML (Document document, String xpathExpression)
          throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    return (Boolean) xPath.compile(xpathExpression).evaluate(document, XPathConstants.BOOLEAN);
  }

  /**
   * Reads xml file and creates xml document object.
   * @param filePath the location of the file.
   * @return xml document.
   * @throws ParserConfigurationException ParserConfigurationException
   * @throws IOException IOException
   * @throws SAXException SAXException
   */
  public static Document getXmlDocument(String filePath) throws ParserConfigurationException, IOException, SAXException {
    filePath = VariablesTransformer.transformSingleValue(filePath);
    System.setProperty(DOCUMENTBUILDERFACTORY, DOCUMENTBUILDERFACTORYIMPL);
    DocumentBuilderFactory docBuilder = DocumentBuilderFactory.newInstance();
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    docBuilder.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    // protect against Denial of Service attack and remote file access
    docBuilder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return docBuilder.newDocumentBuilder().parse(filePath);
  }
}
