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
package org.hibernate.search.engine;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

import org.hibernate.search.bridge.LuceneOptions;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 * This is a package level class
 *  
 * @author Hardy Ferentschik
 */
class LuceneOptionsImpl implements LuceneOptions {
	private final Store store;
	private final Index index;
	private final TermVector termVector;
	private final Float boost;

	public LuceneOptionsImpl(Store store, Index index, TermVector termVector, Float boost) {
		this.store = store;
		this.index = index;
		this.termVector = termVector;
		this.boost = boost;
	}

	public Store getStore() {
		return store;
	}

	public Index getIndex() {
		return index;
	}

	public TermVector getTermVector() {
		return termVector;
	}

	/**
	 * @return the boost value. If <code>boost == null</code>, the default boost value
	 * 1.0 is returned.
	 */
	public Float getBoost() {
		if ( boost != null ) {
			return boost;
		} else {
			return 1.0f;
		}
	}
}
