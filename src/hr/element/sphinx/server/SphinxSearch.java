package hr.element.sphinx.server;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sphx.api.SphinxClient;
import org.sphx.api.SphinxException;
import org.sphx.api.SphinxMatch;
import org.sphx.api.SphinxResult;
import org.sphx.api.SphinxWordInfo;

public class SphinxSearch {
  protected final String hostname;
  protected int port;
	
  public SphinxSearch(final String hostname, int port) {
	this.hostname = hostname;
	this.port = port;
  }

  public String search(final String index, final String text) throws SphinxException {
	return this.search(index, text, null, null, null);
  }
  
  public String search(final String index, final String text, Integer limit) throws SphinxException {
	return this.search(index, text, limit, null, null);
  }
  
  protected String parseOrder(String[] order)
  {
	List<String> result = new LinkedList<String>();

	if (order == null || order.length == 0)
		order = ArrayUtils.add(order, "!@weight");

	for (String ord : order) {
	  boolean reversed = ord.length() > 0 && ord.charAt(0) == '!';
	  final String key = reversed ? ord.substring(1) : ord;
	  result.add(key + (reversed ? " DESC" : " ASC"));
	}

	return StringUtils.join(result, ", ");
  }
  
  protected Object getAttribute(int type, Object value)
  {
      switch (type){
        case SphinxClient.SPH_ATTR_MULTI:
        case SphinxClient.SPH_ATTR_MULTI64:
          long[] attrM = (long[]) value;
          JSONArray attrMjs = new JSONArray();
          for (int j = 0; j < attrM.length; j++)
            attrMjs.add(attrM[j]);
          return attrMjs;

        case SphinxClient.SPH_ATTR_INTEGER:
        case SphinxClient.SPH_ATTR_ORDINAL:
        case SphinxClient.SPH_ATTR_FLOAT:
        case SphinxClient.SPH_ATTR_BIGINT:
        case SphinxClient.SPH_ATTR_STRING:
          return value;

        case SphinxClient.SPH_ATTR_TIMESTAMP:
          Long iStamp = (Long) value;
          Date date = new Date(iStamp.longValue()*1000);
          return date.toString();

        default:
          return null;
      }
  }
  
  protected Map<String, Object> getAttributes(SphinxResult res, SphinxMatch match)
  {
	HashMap<String, Object> map = new HashMap<String, Object>();
    for (int i = 0; i < res.attrNames.length; i++) {
      map.put(res.attrNames[i], getAttribute(res.attrTypes[i], match.attrValues.get(i)));
    }
    return map;
  }
  
  @SuppressWarnings("unchecked")
  public String search (final String index, final String text, Integer limit, Integer offset, String[] order) throws SphinxException {
    int mode = SphinxClient.SPH_MATCH_ALL;
    int sortMode = SphinxClient.SPH_SORT_EXTENDED;
    
    int _limit = limit == null ? 20 : limit;
    int _offset = offset == null ? 0 : offset;

    String sortClause = parseOrder(order);
    String groupBy = "";
    String groupSort = "";
    
    System.out.println("sortClause: [" + sortClause + "]");

    SphinxClient cl = new SphinxClient();

    cl.SetServer ( this.hostname, this.port );
    cl.SetWeights ( new int[] { 100, 1 } );
    cl.SetMatchMode ( mode );
    cl.SetLimits ( _offset, _limit );
    cl.SetSortMode ( sortMode, sortClause );

    if ( groupBy.length() > 0 )
      cl.SetGroupBy ( groupBy, SphinxClient.SPH_GROUPBY_ATTR, groupSort );

    SphinxResult res = cl.Query(text, index);
    if ( res == null ){
      System.err.println ( "Error: " + cl.GetLastError() );
      return "Error: " + cl.GetLastError();
      //System.exit ( 1 );
    }

    if ( cl.GetLastWarning() != null && cl.GetLastWarning().length() > 0 )
      System.out.println ( "WARNING: " + cl.GetLastWarning() + "\n" );

    /* print me out */
    System.out.println ( "Query '" + text + "' retrieved " + res.total + " of " + res.totalFound + " matches in " + res.time + " sec." );
    System.out.println ( "Query stats:" );
    for ( int i = 0; i < res.words.length; i++ ){
      SphinxWordInfo wordInfo = res.words[i];
      System.out.println ( "\t'" + wordInfo.word + "' found " + wordInfo.hits + " times in " + wordInfo.docs + " documents" );
    }
    
    final JSONObject result = new JSONObject();
    final JSONArray matches = new JSONArray();
    result.put("matches", matches);
    result.put("count", res.totalFound);

    System.out.println ( "\nMatches:" );
    for (int i = 0; i < res.matches.length; i++) {
      SphinxMatch match = res.matches[i];
      System.out.print ( (i+1) + ". id=" + match.docId + ", weight=" + match.weight );

      if ( res.attrNames == null || res.attrTypes == null )
        continue;
      
      Map<String, Object> attrs = getAttributes(res, match);

      JSONObject jsobj = new JSONObject();
      jsobj.put("weight", match.weight);
      jsobj.put("pk", attrs.get("pk"));

      matches.add(jsobj);
      System.out.println();
    }

    return result.toString();
  }
}
