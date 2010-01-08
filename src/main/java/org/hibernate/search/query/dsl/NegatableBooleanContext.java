package org.hibernate.search.query.dsl;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

/**
 * // TODO: Document this
 *
 * @author Navin Surtani
 */


public class NegatableBooleanContext {
   private final BooleanContext delegate;

   public NegatableBooleanContext(BooleanClause.Occur occur) {
      this.delegate = new BooleanContext(occur);
   }

   public NegatableBooleanContext not() {
      BooleanClause.Occur present = delegate.getOccur();
      if ( present == null ) {
         //assertion exception
      }
      else if (present == BooleanClause.Occur.SHOULD) {
         //assertion exception
      }
      else if ( present == BooleanClause.Occur.MUST) {
         delegate.setOccur(BooleanClause.Occur.MUST_NOT);
      }
      else if (present == BooleanClause.Occur.MUST_NOT) {
         delegate.setOccur(BooleanClause.Occur.MUST);
      }
      else {
         //asseriton failure
      }
      return this;
   }

   public NegatableBooleanContext add(Query clause) {
      delegate.add(clause);
      return this;
   }

   public Query createQuery() {
      return delegate.createQuery();
   }
}
