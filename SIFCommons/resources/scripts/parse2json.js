// Added by John W. Lovell/2016 for Java invocation from JavaX.

// Parse and pass to Goessner's function.
function parse2json(stream, tab) {
  var dom = parseXML(stream);
  return xml2json(dom.getDocumentElement(), tab);
}

function parseXML(stream) {
  var dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
  var db = dbf.newDocumentBuilder();
  var document = db.parse(stream);
  //dumpXml(document);  // Debug
  return document;
}

// So we can debug
function dumpXml(node) {
  var transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
  transformer.transform(
    new javax.xml.transform.dom.DOMSource(node),
    new javax.xml.transform.stream.StreamResult(java.lang.System.out)
  );
}
