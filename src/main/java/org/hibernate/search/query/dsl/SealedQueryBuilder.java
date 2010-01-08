package org.hibernate.search.query.dsl;

import org.apache.lucene.search.BooleanClause;

/**
 * // TODO: Document this
 *
 * @author Navin Surtani
 */


public class SealedQueryBuilder {

   public SealedQueryBuilder(){

   }

   public BooleanContext should() {
      return new BooleanContext(BooleanClause.Occur.SHOULD);
   }

   public NegatableBooleanContext must(){
      return new NegatableBooleanContext(BooleanClause.Occur.MUST);
   }

   public UnbuildableTermQueryBuilderOnField term(){
      return new UnbuildableTermQueryBuilderOnField();
   }
}
