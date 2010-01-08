package org.hibernate.search.query.dsl;

import org.apache.lucene.search.BooleanClause;

/**
 * Starting class that will allow users to build their queries using the DSL.
 *
 * //TODO: This needs to be tied into the SearchFactory somehow so that users can actually "access" it.
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
