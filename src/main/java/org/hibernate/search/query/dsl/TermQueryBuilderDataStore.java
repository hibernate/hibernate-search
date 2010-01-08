package org.hibernate.search.query.dsl;

import org.apache.lucene.index.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will just store the required terms.
 *
 * @author Navin Surtani
 */


public class TermQueryBuilderDataStore {

   private List<Term> terms;

   public TermQueryBuilderDataStore(){
      terms = new ArrayList<Term>();
   }

   public List<Term> getTerms(){
      return terms;
   }

   public void addTerm(Term term){
      terms.add(term);
   }

}
