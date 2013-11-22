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

package org.hibernate.search.query.dsl;

/**
 * Builds up Lucene queries for a given entity type following the fluent API pattern. The resulting {@link org.apache.lucene.search.Query} can
 * be obtained from the final {@link Termination} object of the invocation chain.
 * </p>
 * If required, the resulting {@code Query} instance can be modified or combined with other queries, be them created
 * via this fluent API or using native Lucene APIs.
 *
 * @author Emmanuel Bernard
 */
public interface QueryBuilder {
	/**
	 * Build a term query (see {@link org.apache.lucene.search.TermQuery}).
	 *
	 * @return a {@code TermContext} instance for building the term query
	 */
	TermContext keyword();

	/**
	 * Build a range query (see {@link org.apache.lucene.search.TermRangeQuery}.
	 *
	 * @return a {@code RangeContext} instance for building the range query
	 */
	RangeContext range();

	/**
	 * Build a phrase query (see {@link org.apache.lucene.search.PhraseQuery}).
	 *
	 * @return a {@code PhraseContext} instance for building the range query
	 */
	PhraseContext phrase();

	/**
	 * Start for building a boolean query.
	 *
	 * @return a {@code BooleanJunction} instance for building the boolean query
	 */
	BooleanJunction<BooleanJunction> bool();

	/**
	 * Query matching all documents (typically mixed with a boolean query).
	 *
	 * @return an {@code AllContext}
	 */
	AllContext all();

	/**
	 * Build a facet request
	 *
	 * @return the facet context as entry point for building the facet request
	 */
	FacetContext facet();

	/**
	 * Build a spatial query
	 */
	SpatialContext spatial();
}
