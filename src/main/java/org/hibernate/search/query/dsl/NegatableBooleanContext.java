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
import org.apache.lucene.search.Query;

/**
 * // TODO: Document this
 *
 * @author Navin Surtani
 */
public class NegatableBooleanContext {
	
   private final BooleanContext delegate;

   public NegatableBooleanContext(BooleanClause.Occur occur) {
      this.delegate = new BooleanContext( occur );
   }

   public NegatableBooleanContext not() {
      BooleanClause.Occur present = delegate.getOccur();
      if ( present == null ) {
         //assertion exception
      }
      else if (present == BooleanClause.Occur.SHOULD) {
         //assertion exception
      }
      else if ( present == BooleanClause.Occur.MUST) {
         delegate.setOccur(BooleanClause.Occur.MUST_NOT);
      }
      else if (present == BooleanClause.Occur.MUST_NOT) {
         delegate.setOccur(BooleanClause.Occur.MUST);
      }
      else {
         //assert failure
      }
      return this;
   }

   public NegatableBooleanContext add(Query clause) {
      delegate.add(clause);
      return this;
   }

   public Query createQuery() {
      return delegate.createQuery();
   }
   
}
