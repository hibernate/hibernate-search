/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactoryContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContextExtension;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl.ElasticsearchSearchPredicateFactoryContextImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl.ElasticsearchSearchSortContainerContextImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * An extension for the Elasticsearch backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 */
public final class ElasticsearchExtension
		implements SearchPredicateFactoryContextExtension<ElasticsearchSearchPredicateFactoryContext>,
		SearchSortContainerContextExtension<ElasticsearchSearchSortContainerContext>,
		IndexSchemaFieldContextExtension<ElasticsearchIndexSchemaFieldContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ElasticsearchExtension INSTANCE = new ElasticsearchExtension();

	public static ElasticsearchExtension get() {
		return INSTANCE;
	}

	private ElasticsearchExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<ElasticsearchSearchPredicateFactoryContext> extendOptional(
			SearchPredicateFactoryContext original, SearchPredicateBuilderFactory<C, B> factory) {
		if ( factory instanceof ElasticsearchSearchPredicateBuilderFactory ) {
			return Optional.of( new ElasticsearchSearchPredicateFactoryContextImpl(
					original, (ElasticsearchSearchPredicateBuilderFactory) factory
			) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<ElasticsearchSearchSortContainerContext> extendOptional(
			SearchSortContainerContext original, SearchSortBuilderFactory<C, B> factory,
			SearchSortDslContext<? super B> dslContext) {
		if ( factory instanceof ElasticsearchSearchSortBuilderFactory ) {
			return Optional.of( extendUnsafe( original, (ElasticsearchSearchSortBuilderFactory) factory, dslContext ) );
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

	@SuppressWarnings("unchecked") // If the target is Elasticsearch, then we know B = ElasticsearchSearchSortBuilder
	private <B> ElasticsearchSearchSortContainerContext extendUnsafe(
			SearchSortContainerContext original, ElasticsearchSearchSortBuilderFactory factory,
			SearchSortDslContext<? super B> dslContext) {
		return new ElasticsearchSearchSortContainerContextImpl(
				original, factory,
				(SearchSortDslContext<? super ElasticsearchSearchSortBuilder>) dslContext
		);
	}
}
