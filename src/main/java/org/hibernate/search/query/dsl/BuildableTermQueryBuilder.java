package org.hibernate.search.query.dsl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.List;

/**
 * Class that will allow the user to actually build his query.
 *
 * @author Navin Surtani
 */


public class BuildableTermQueryBuilder extends AbstractTermQueryBuilder {
   public BuildableTermQueryBuilder(TermQueryBuilderDataStore dataStore) {
      this.dataStore = dataStore;
   }

   public UnbuildableTermQueryBuilderOnSearch on(String field) {
      return new UnbuildableTermQueryBuilderOnSearch(dataStore, field);
   }

   public Query build() {
      // Start by getting the lists of fields and searches.
      List<Term> terms = dataStore.getTerms();

      //TODO:- This kind of sucks. How can we do this nicely?
      // Create a TermQuery for the first term.
      Query tq = new TermQuery(terms.get(0));

      // Now create an array of TermQueries for me to do the combine later on.
      // The array size will be 1 less than that of the list.
      TermQuery[] termQueries = new TermQuery[terms.size() - 1];

      // Loop through the rest of the list.
      for (int i = 1; i<terms.size(); i++){
         // The index of each newly created TermQuery in the array will always be 1 less than that of the list
         // This is because the first term in the list has already been dealt with, so the first termQuery in the array
         // will correspond to the second term from the list.

         termQueries[i - 1] = new TermQuery(terms.get(i));
      }

      tq = tq.combine(termQueries);
      return tq;
   }
}
