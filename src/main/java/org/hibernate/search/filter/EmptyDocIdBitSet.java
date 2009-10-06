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
package org.hibernate.search.filter;

import java.io.Serializable;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * A DocIdSet which is always empty.
 * Stateless and ThreadSafe.
 * 
 * @author Sanne Grinovero
 */
public final class EmptyDocIdBitSet extends DocIdSet implements Serializable {

	private static final long serialVersionUID = 6429929383767238322L;

	public static final DocIdSet instance = new EmptyDocIdBitSet();
	
	private static final DocIdSetIterator iterator = new EmptyDocIdSetIterator();
	
	private EmptyDocIdBitSet(){
		// is singleton
	}

	@Override
	public final DocIdSetIterator iterator() {
		return iterator;
	}

	/**
	 * implements a DocIdSetIterator for an empty DocIdSet
	 * As it is empty it also is stateless and so it can be reused.
	 */
	private static final class EmptyDocIdSetIterator extends DocIdSetIterator {

		@Override
		public final int doc() {
			throw new IllegalStateException( "Should never be called" );
		}

		@Override
		public final boolean next() {
			return false;
		}

		@Override
		public final boolean skipTo(int target) {
			return false;
		}

	}
	
}
