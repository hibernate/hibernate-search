/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.hibernate.search.query.dsl.sort.SortContext;

/**
 * Builds up Lucene queries for a given entity type following the fluent API pattern.
 *
 * <p>
 * The resulting {@link org.apache.lucene.search.Query} can
 * be obtained from the final {@link Termination} object of the invocation chain.
 * </p>
 * If required, the resulting {@code Query} instance can be modified or combined with other queries created
 * via this fluent API or via the native Lucene API.
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
	 * @return a {@code PhraseContext} instance for building the phrase query
	 */
	PhraseContext phrase();

	/**
	 * Build a query from a simple query string.
	 *
	 * @return a {@code SimpleQueryStringContext} instance for building a query from a simple query string
	 */
	SimpleQueryStringContext simpleQueryString();

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
	 * Build a facet query.
	 *
	 * @return the facet context as entry point for building the facet request
	 */
	FacetContext facet();

	/**
	 * Build a spatial query.
	 * @return the spatial context as entry point got building the spatial request
	 */
	SpatialContext spatial();

	/**
	 * Build a query matching resembling content.
	 *
	 * It uses an approach similar to Lucene's {@code MoreLikeThis}
	 *
	 * @return the entry point for building a more like this query
	 */
	MoreLikeThisContext moreLikeThis();

	/**
	 * Build a sort that can be applied to a query execution.
	 * When multiple sort definitions are expressed,
	 * they are processed in decreasing priority.
	 *
	 * @return the entry point for building a sort
	 */
	SortContext sort();

}
