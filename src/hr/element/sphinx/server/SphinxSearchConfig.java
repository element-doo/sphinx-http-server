package hr.element.sphinx.server;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class SphinxSearchConfig {
  protected String hostname;
  protected int port;
  protected HashMap<String, HashMap<String, Integer>> weights;

  public SphinxSearchConfig(final String configPath) {
    weights = new HashMap<String, HashMap<String, Integer>>();
    try {
      final File configFile = new File(configPath);
      final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configFile);
      doc.getDocumentElement().normalize();
      final NodeList inodeList = doc.getDocumentElement().getChildNodes();
      final int inodeListLength = inodeList.getLength();
      for (int i = 0; i < inodeListLength; i++) {
        final Node inode = inodeList.item(i);
        final String inodeName = inode.getNodeName();
        if (inodeName == "hostname")
          hostname = inode.getTextContent();
        if (inodeName == "port")
          port = Integer.parseInt(inode.getTextContent());
        if (inodeName == "weights") {
          final NodeList jnodeList = inode.getChildNodes();
          final int jnodeListLength = jnodeList.getLength();
          for (int j = 0; j < jnodeListLength; j++) {
            final Node jnode = jnodeList.item(j);
            if (jnode.getNodeName() != "index")
              continue;

            final String indexName = jnode.getAttributes().getNamedItem("name").getTextContent();
            if (weights.containsKey(indexName)) {
              System.err.println("Warning: weight specification for Index '" + indexName + "' already exists. Skipping.");
              continue;
            }
            HashMap<String,Integer> indexWeights = new HashMap<String,Integer>();
            final NodeList knodeList = jnode.getChildNodes();
            final int knodeListLength = knodeList.getLength();
            for (int k = 0; k < knodeListLength; k++) {
              final Node knode = knodeList.item(k);
              if (knode.getNodeName() != "weight")
                continue;

              final NamedNodeMap knodeAttr = knode.getAttributes();
              final String weightFor = knodeAttr.getNamedItem("for").getTextContent();
              if (indexWeights.containsKey(weightFor)) {
                System.err.println("Warning: weight specification for Field '" + weightFor + "' in Index '" + indexName + "' already exists. Skipping.");
                continue;
              }
              final int weightIs = Integer.parseInt(knodeAttr.getNamedItem("is").getTextContent());

              indexWeights.put(weightFor, weightIs);
            }
            weights.put(indexName, indexWeights);
          }
        }
      }
    } catch (Exception e) {
      System.err.print("Couldn't parse config xml file with message: ");
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(255);
    }
  }

  public final String getHostname() { return this.hostname; }
  public final int getPort() { return this.port; }
  public final HashMap<String, HashMap<String, Integer>> getWeights() { return this.weights; }
}
