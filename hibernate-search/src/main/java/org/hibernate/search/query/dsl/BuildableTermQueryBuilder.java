/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
