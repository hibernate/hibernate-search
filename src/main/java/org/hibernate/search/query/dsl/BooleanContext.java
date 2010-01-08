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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that allows users to create BooleanQueries.
 *
 * @author Navin Surtani
 */

//TODO do we want a QueryCreator interface with T extends Query and T createQuery() ?
public class BooleanContext {
	
   private BooleanClause.Occur occur;
   // List has an allocation of 5 temporarily so that it's not created with an arbitrary one.
   private final List<Query> clauses = new ArrayList<Query>(5);

   public BooleanContext(BooleanClause.Occur occur) {
      this.occur = occur;
   }

   public BooleanContext add(Query clause) {
      clauses.add( clause );
      return this;
   }

   public Query createQuery() {
      BooleanQuery boolQuery = new BooleanQuery();
      for (Query clause : clauses) {
         boolQuery.add( clause, occur );
      }
      return boolQuery;
   }

   protected void setOccur(BooleanClause.Occur occur) {
      this.occur = occur;
   }

   protected BooleanClause.Occur getOccur() {
      return occur;
   }

}
