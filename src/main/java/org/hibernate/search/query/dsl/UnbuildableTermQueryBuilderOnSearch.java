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

/**
 * Class that allows users to continue building their TermQueries.
 * However, a TermQuery cannot be built from an instance of this class, as there is not enough information
 * to do so.
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
