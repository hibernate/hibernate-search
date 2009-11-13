package org.hibernate.search.query.dsl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Class that will be called by the {@link org.hibernate.search.query.dsl.SealedQueryBuilder} each time someone wants
 * to create a new query instance.
 *
 * @author Navin Surtani
 */


public class QueryContext {

   private Analyzer analyzer;
   private String field;
   private String search; 

   public QueryContext search(String mySearch){
      this.search = mySearch;
      return this;
   }

   public QueryContext onField(String onField){
      this.field = onField;
      return this;
   }

   public QueryContext withAnalyzer(Analyzer analyzer){
      this.analyzer = analyzer;
      return this;
   }

   public Query build() throws ParseException {
      QueryParser parser;

      if (analyzer != null){
         parser = new QueryParser(field, analyzer);
      }
      else{
         // Do we just use a StandardAnalyzer?
         parser = new QueryParser(field, new StandardAnalyzer());
      }

      return parser.parse(search);
   }

}
