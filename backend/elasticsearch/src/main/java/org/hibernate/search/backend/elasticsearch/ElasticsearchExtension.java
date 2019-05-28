/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultDefinitionContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.projection.impl.ElasticsearchSearchProjectionFactoryContextImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.impl.ElasticsearchSearchQueryResultDefinitionContextImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexSearchScope;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContextExtension;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl.ElasticsearchSearchPredicateFactoryContextImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl.ElasticsearchSearchSortContainerContextImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.backend.scope.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An extension for the Elasticsearch backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @param <H> The type of query hits.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * @param <R> The reference type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <E> The entity type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 */
public final class ElasticsearchExtension<H, R, E>
		implements SearchQueryContextExtension<ElasticsearchSearchQueryResultDefinitionContext<R, E>, R, E>,
		SearchQueryExtension<ElasticsearchSearchQuery<H>, H>,
		SearchPredicateFactoryContextExtension<ElasticsearchSearchPredicateFactoryContext>,
		SearchSortContainerContextExtension<ElasticsearchSearchSortContainerContext>,
		SearchProjectionFactoryContextExtension<ElasticsearchSearchProjectionFactoryContext<R, E>, R, E>,
		IndexFieldTypeFactoryContextExtension<ElasticsearchIndexFieldTypeFactoryContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ElasticsearchExtension<Object, Object, Object> INSTANCE = new ElasticsearchExtension<>();

	@SuppressWarnings("unchecked") // The instance works for any H, R and E
	public static <H, R, E> ElasticsearchExtension<H, R, E> get() {
		return (ElasticsearchExtension<H, R, E>) INSTANCE;
	}

	private ElasticsearchExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchQueryResultDefinitionContext<R, E>> extendOptional(
			SearchQueryResultDefinitionContext<?, R, E, ?, ?> original,
			IndexSearchScope<?> indexSearchScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		if ( indexSearchScope instanceof ElasticsearchIndexSearchScope ) {
			return Optional.of( new ElasticsearchSearchQueryResultDefinitionContextImpl<>(
					(ElasticsearchIndexSearchScope) indexSearchScope, sessionContext, loadingContextBuilder
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
	public Optional<ElasticsearchSearchQuery<H>> extendOptional(SearchQuery<H> original,
			LoadingContext<?, ?> loadingContext) {
		if ( original instanceof ElasticsearchSearchQuery ) {
			return Optional.of( (ElasticsearchSearchQuery<H>) original );
		}
		else {
			return Optional.empty();
		}
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
	public Optional<ElasticsearchSearchProjectionFactoryContext<R, E>> extendOptional(
			SearchProjectionFactoryContext<R, E> original, SearchProjectionBuilderFactory factory) {
		if ( factory instanceof ElasticsearchSearchProjectionBuilderFactory ) {
			return Optional.of( new ElasticsearchSearchProjectionFactoryContextImpl<>(
					original, (ElasticsearchSearchProjectionBuilderFactory) factory
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
	public ElasticsearchIndexFieldTypeFactoryContext extendOrFail(IndexFieldTypeFactoryContext original) {
		if ( original instanceof ElasticsearchIndexFieldTypeFactoryContext ) {
			return (ElasticsearchIndexFieldTypeFactoryContext) original;
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
