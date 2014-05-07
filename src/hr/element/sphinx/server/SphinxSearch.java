package hr.element.sphinx.server;

import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sphx.api.SphinxClient;
import org.sphx.api.SphinxException;
import org.sphx.api.SphinxMatch;
import org.sphx.api.SphinxResult;
import org.sphx.api.SphinxWordInfo;

public class SphinxSearch {

  private final String index;
  private final String text;

  public SphinxSearch(final String index, final String text) {
    this.index = index;
    this.text = text;
  }

  @SuppressWarnings("unchecked")
  public String search () throws SphinxException{

    String host = "localhost";
    int port = 9312;
    int mode = SphinxClient.SPH_MATCH_ALL;
    int offset = 0;
    int limit = 20;
    int sortMode = SphinxClient.SPH_SORT_RELEVANCE;
    String sortClause = "";
    String groupBy = "";
    String groupSort = "";

    SphinxClient cl = new SphinxClient();

    cl.SetServer ( host, port );
    cl.SetWeights ( new int[] { 100, 1 } );
    cl.SetMatchMode ( mode );
    cl.SetLimits ( offset, limit );
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

    final JSONArray jsonArray = new JSONArray();
    JSONObject match;

    System.out.println ( "\nMatches:" );
    for ( int i = 0; i < res.matches.length; i++ ){
      SphinxMatch info = res.matches[i];
      System.out.print ( (i+1) + ". id=" + info.docId + ", weight=" + info.weight );

      if ( res.attrNames == null || res.attrTypes == null )
        continue;

      match = new JSONObject();

      match.put("weight", info.weight);

      for ( int a = 0; a < res.attrNames.length; a++ ){

        StringBuilder builder = new StringBuilder();
        System.out.print ( ", " + res.attrNames[a] + "=" );

        if ( res.attrTypes[a] == SphinxClient.SPH_ATTR_MULTI || res.attrTypes[a] == SphinxClient.SPH_ATTR_MULTI64 ){
          System.out.print ( "(" );
          long[] attrM = (long[]) info.attrValues.get(a);
          if ( attrM!=null )
            for ( int j = 0; j < attrM.length; j++ ){
            if ( j != 0 ) { System.out.print ( "," ); builder.append(","); }
            System.out.print ( attrM[j] );
            builder.append(attrM[j]);
          }
          System.out.print ( ")" );

        } else{
          switch ( res.attrTypes[a] ){
            case SphinxClient.SPH_ATTR_INTEGER:
            case SphinxClient.SPH_ATTR_ORDINAL:
            case SphinxClient.SPH_ATTR_FLOAT:
            case SphinxClient.SPH_ATTR_BIGINT:
            case SphinxClient.SPH_ATTR_STRING:
              /* ints, longs, floats, strings.. print as is */
              System.out.print ( info.attrValues.get(a) );
              builder.append(info.attrValues.get(a));
              break;

            case SphinxClient.SPH_ATTR_TIMESTAMP:
              Long iStamp = (Long) info.attrValues.get(a);
              Date date = new Date ( iStamp.longValue()*1000 );
              System.out.print ( date.toString() );
              builder.append(date.toString());
              break;

            default:
              System.out.print ( "(unknown-attr-type=" + res.attrTypes[a] + ")" );
          }
        }
        System.out.print("");
        match.put(res.attrNames[a], builder.toString());
      }
      jsonArray.add(match);
      System.out.println();
    }
    return jsonArray.toString();
  }

}
