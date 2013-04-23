/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.annotations;

/**
 * Cache mode strategy for <code>FullTextFilterDef</code>s.
 *
 * @see FullTextFilterDef
 * @author Emmanuel Bernard
 */
public enum FilterCacheModeType {
	/**
	 * No filter instance and no result is cached by Hibernate Search.
	 * For every filter call, a new filter instance is created.
	 */
	NONE,

	/**
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.getDocIdSet()</code> calls.
	 * Results are not cached by Hibernate Search.
	 *
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)

	 */
	INSTANCE_ONLY,

	/**
	 * Both the filter instance and the <code>DocIdSet</code> results are cached.
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.getDocIdSet()</code> calls.
	 * <code>DocIdSet</code> results are cached per <code>IndexReader</code>.
	 *
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader) 
	 */
	INSTANCE_AND_DOCIDSETRESULTS

}
