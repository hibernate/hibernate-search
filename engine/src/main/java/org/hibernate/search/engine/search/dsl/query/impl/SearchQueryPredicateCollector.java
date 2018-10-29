/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.RootSearchPredicateDslContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.query.SearchQueryWrappingDefinitionResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

/**
 * Collect a search predicate to later add it to a search query.
 * <p>
 * This class is essentially a bridge transferring information from {@link SearchQueryWrappingDefinitionResultContext}
 * to {@link SearchQueryBuilder#getQueryElementCollector()}.
 */
class SearchQueryPredicateCollector<C, B> {

	private final SearchPredicateFactory<C, B> factory;
	private final RootSearchPredicateDslContextImpl<B> rootDslContext;

	SearchQueryPredicateCollector(SearchPredicateFactory<C, B> factory) {
		this.factory = factory;
		this.rootDslContext = new RootSearchPredicateDslContextImpl<>( factory );
	}

	void contribute(C collector) {
		factory.contribute( collector, rootDslContext.getResultingBuilder() );
	}

	void collect(SearchPredicate predicate) {
		rootDslContext.addChild( factory.toImplementation( predicate ) );
	}

	void collect(Consumer<? super SearchPredicateContainerContext<SearchPredicate>> dslPredicateContributor) {
		SearchPredicateContainerContext<SearchPredicate> containerContext =
				new SearchPredicateContainerContextImpl<>( factory, rootDslContext );
		dslPredicateContributor.accept( containerContext );
	}
}
