/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
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
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a {@link SearchQuery} with {@code org.hibernate.search.mapper.orm.session.SearchSession#search(Class)},
 * and define your predicates using {@link SearchQueryWhereStep#where(Function)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface QueryBuilder {
	/**
	 * Build a term query (see {@link org.apache.lucene.search.TermQuery}).
	 *
	 * @return a {@code TermContext} instance for building the term query
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#match()}.
	 * For wildcard predicates, use {@link SearchPredicateFactory#wildcard()}.
	 */
	@Deprecated
	TermContext keyword();

	/**
	 * Build a range query (see {@link org.apache.lucene.search.TermRangeQuery}.
	 *
	 * @return a {@code RangeContext} instance for building the range query
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#range()}.
	 */
	@Deprecated
	RangeContext range();

	/**
	 * Build a phrase query (see {@link org.apache.lucene.search.PhraseQuery}).
	 *
	 * @return a {@code PhraseContext} instance for building the phrase query
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#phrase()}.
	 */
	@Deprecated
	PhraseContext phrase();

	/**
	 * Build a query from a simple query string.
	 *
	 * @return a {@code SimpleQueryStringContext} instance for building a query from a simple query string
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#simpleQueryString()}.
	 */
	@Deprecated
	SimpleQueryStringContext simpleQueryString();

	/**
	 * Start for building a boolean query.
	 *
	 * @return a {@code BooleanJunction} instance for building the boolean query
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#bool()}.
	 */
	@Deprecated
	BooleanJunction<BooleanJunction> bool();

	/**
	 * Query matching all documents (typically mixed with a boolean query).
	 *
	 * @return an {@code AllContext}
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#matchAll()}.
	 */
	@Deprecated
	AllContext all();

	/**
	 * Build a facet query.
	 *
	 * @return the facet context as entry point for building the facet request
	 * @deprecated See the deprecation note on {@link FacetContext}.
	 */
	@Deprecated
	FacetContext facet();

	/**
	 * Build a spatial query.
	 * @return the spatial context as entry point got building the spatial request
	 * @deprecated See the javadoc of this class for how to create predicates in Hibernate Search 6.
	 * The equivalent predicate in Hibernate Search 6 is {@link SearchPredicateFactory#spatial()}.
	 */
	SpatialContext spatial();

	/**
	 * Build a sort that can be applied to a query execution.
	 * When multiple sort definitions are expressed,
	 * they are processed in decreasing priority.
	 *
	 * @return the entry point for building a sort
	 * @deprecated See the deprecation note on {@link SortContext}.
	 */
	@Deprecated
	SortContext sort();

}
