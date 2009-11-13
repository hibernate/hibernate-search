package org.hibernate.search.query.dsl;

/**
 * // TODO: Document this
 *
 * @author Navin Surtani
 */


public class SealedQueryBuilder {

   public SealedQueryBuilder(){

   }

   public QueryContext getContext(){
      return new QueryContext();
   }
}
