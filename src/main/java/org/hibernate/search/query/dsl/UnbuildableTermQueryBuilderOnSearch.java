package org.hibernate.search.query.dsl;

import org.apache.lucene.index.Term;

/**
 * // TODO: Document this
 *
 * @author Navin Surtani
 */


public class UnbuildableTermQueryBuilderOnSearch extends AbstractTermQueryBuilder {

   private String field;

   public UnbuildableTermQueryBuilderOnSearch(TermQueryBuilderDataStore dataStore, String field) {
      this.dataStore = dataStore;
      this.field = field;
   }

   public BuildableTermQueryBuilder matches(String search) {
      // Now that I've got enough information to create a term I can do so
      Term term = new Term(field, search);
      dataStore.addTerm(term);
      // return the Buildable type.
      return new BuildableTermQueryBuilder(dataStore);
   }


}
