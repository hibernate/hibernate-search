/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContextExtension;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl.ElasticsearchSearchPredicateContainerContextImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl.ElasticsearchSearchSortContainerContextImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * An extension for the Elasticsearch backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 */
public final class ElasticsearchExtension<N>
		implements SearchPredicateContainerContextExtension<N, ElasticsearchSearchPredicateContainerContext<N>>,
		SearchSortContainerContextExtension<N, ElasticsearchSearchSortContainerContext<N>>,
		IndexSchemaFieldContextExtension<ElasticsearchIndexSchemaFieldContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@SuppressWarnings("rawtypes")
	private static final ElasticsearchExtension INSTANCE = new ElasticsearchExtension();

	@SuppressWarnings("unchecked")
	public static <N> ElasticsearchExtension<N> get() {
		return INSTANCE;
	}

	private ElasticsearchExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<ElasticsearchSearchPredicateContainerContext<N>> extendOptional(
			SearchPredicateContainerContext<N> original, SearchPredicateFactory<C, B> factory,
			SearchPredicateDslContext<N, ? super B> dslContext) {
		if ( factory instanceof ElasticsearchSearchPredicateFactory ) {
			return Optional.of( extendUnsafe( original, (ElasticsearchSearchPredicateFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> ElasticsearchSearchSortContainerContext<N> extendOrFail(SearchSortContainerContext<N> original,
			SearchSortFactory<C, B> factory, SearchSortDslContext<N, ? super B> dslContext) {
		if ( factory instanceof ElasticsearchSearchSortFactory ) {
			return extendUnsafe( original, (ElasticsearchSearchSortFactory) factory, dslContext );
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( factory );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<ElasticsearchSearchSortContainerContext<N>> extendOptional(
			SearchSortContainerContext<N> original, SearchSortFactory<C, B> factory,
			SearchSortDslContext<N, ? super B> dslContext) {
		if ( factory instanceof ElasticsearchSearchSortFactory ) {
			return Optional.of( extendUnsafe( original, (ElasticsearchSearchSortFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ElasticsearchIndexSchemaFieldContext extendOrFail(IndexSchemaFieldContext original) {
		if ( original instanceof ElasticsearchIndexSchemaFieldContext ) {
			return (ElasticsearchIndexSchemaFieldContext) original;
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( original );
		}
	}

	@SuppressWarnings("unchecked") // If the target is Elasticsearch, then we know B = ElasticsearchSearchPredicateBuilder
	private <B> ElasticsearchSearchPredicateContainerContext<N> extendUnsafe(
			SearchPredicateContainerContext<N> original, ElasticsearchSearchPredicateFactory factory,
			SearchPredicateDslContext<N, ? super B> dslContext) {
		return new ElasticsearchSearchPredicateContainerContextImpl<>(
				original, factory,
				(SearchPredicateDslContext<N, ? super ElasticsearchSearchPredicateBuilder>) dslContext
		);
	}

	@SuppressWarnings("unchecked") // If the target is Elasticsearch, then we know B = ElasticsearchSearchSortBuilder
	private <B> ElasticsearchSearchSortContainerContext<N> extendUnsafe(
			SearchSortContainerContext<N> original, ElasticsearchSearchSortFactory factory,
			SearchSortDslContext<N, ? super B> dslContext) {
		return new ElasticsearchSearchSortContainerContextImpl<>(
				original, factory,
				(SearchSortDslContext<N, ? super ElasticsearchSearchSortBuilder>) dslContext
		);
	}
}
