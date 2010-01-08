package org.hibernate.search.query.dsl;

/**
 * Class that exposes only the on(String field) method as this class will only be returned to a user when
 * SealedQueryBuilder.term() is called.
 *
 * @author Navin Surtani
 */


public class UnbuildableTermQueryBuilderOnField extends AbstractTermQueryBuilder {
   
   public UnbuildableTermQueryBuilderOnField(){
      dataStore = new TermQueryBuilderDataStore();
   }

   public UnbuildableTermQueryBuilderOnSearch on(String field){
      return new UnbuildableTermQueryBuilderOnSearch(dataStore, field);
   }
}
