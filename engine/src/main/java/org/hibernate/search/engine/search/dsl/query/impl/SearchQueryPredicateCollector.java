/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.query.SearchQueryWrappingDefinitionResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Collect a search predicate to later add it to a search query.
 * <p>
 * This class is essentially a bridge transferring information from {@link SearchQueryWrappingDefinitionResultContext}
 * to {@link SearchQueryBuilder#getQueryElementCollector()}.
 */
class SearchQueryPredicateCollector<C, B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateFactory<C, B> factory;

	private B builder;

	SearchQueryPredicateCollector(SearchPredicateFactory<C, B> factory) {
		this.factory = factory;
	}

	void contribute(C collector) {
		factory.contribute( collector, builder );
	}

	void collect(SearchPredicate predicate) {
		collect( factory.toImplementation( predicate ) );
	}

	void collect(Function<? super SearchPredicateContainerContext, SearchPredicate> dslPredicateContributor) {
		SearchPredicateContainerContext containerContext =
				new SearchPredicateContainerContextImpl<>( factory );
		collect( dslPredicateContributor.apply( containerContext ) );
	}

	private void collect(B builder) {
		if ( this.builder != null ) {
			throw log.cannotAddMultiplePredicatesToQueryRoot();
		}
		this.builder = builder;
	}
}
