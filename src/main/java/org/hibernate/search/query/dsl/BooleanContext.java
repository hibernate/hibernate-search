package org.hibernate.search.query.dsl;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: Document this
 *
 * @author Navin Surtani
 */

//TODO do we want a QueryCreator interface with T extends Query and T createQuery() ?
public class BooleanContext {
   private BooleanClause.Occur occur;
   // List has an allocation of 5 temporarily so that it's not created with an arbitrary one.
   private final List<Query> clauses = new ArrayList<Query>(5);

   public BooleanContext(BooleanClause.Occur occur) {
      this.occur = occur;
   }

   public BooleanContext add(Query clause) {
      clauses.add(clause);
      return this;
   }

   public Query createQuery() {
      BooleanQuery boolQuery = new BooleanQuery();
      for(Query clause : clauses) {
         boolQuery.add(clause, occur);
      }
      return boolQuery;
   }

   protected void setOccur(BooleanClause.Occur occur) {
      this.occur = occur;
   }

   protected BooleanClause.Occur getOccur() {
      return occur;
   }

}
