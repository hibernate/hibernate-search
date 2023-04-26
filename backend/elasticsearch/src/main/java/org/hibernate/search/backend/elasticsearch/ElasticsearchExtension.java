/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaExport;
import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQuerySelectStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.impl.ElasticsearchSearchQuerySelectStepImpl;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryExtension;
import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.engine.common.schema.management.SchemaExportExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An extension for the Elasticsearch backend, giving access to Elasticsearch-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @param <H> The type of query hits.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <R> The entity reference type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <E> The entity type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <LOS> The type of the initial step of the loading options definition DSL.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 *
 * @see #get()
 */
public final class ElasticsearchExtension<H, R, E, LOS>
		implements SearchQueryDslExtension<ElasticsearchSearchQuerySelectStep<R, E, LOS>, R, E, LOS>,
		SearchQueryExtension<ElasticsearchSearchQuery<H>, H>,
		SearchPredicateFactoryExtension<ElasticsearchSearchPredicateFactory>,
		SearchSortFactoryExtension<ElasticsearchSearchSortFactory>,
		SearchProjectionFactoryExtension<ElasticsearchSearchProjectionFactory<R, E>, R, E>,
		SearchAggregationFactoryExtension<ElasticsearchSearchAggregationFactory>,
		IndexFieldTypeFactoryExtension<ElasticsearchIndexFieldTypeFactory>,
		SchemaExportExtension<ElasticsearchIndexSchemaExport> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ElasticsearchExtension<Object, Object, Object, Object> INSTANCE = new ElasticsearchExtension<>();

	/**
	 * Get the extension with generic parameters automatically set as appropriate for the context in which it's used.
	 *
	 * @param <H> The type of query hits.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() )}.
	 * @param <R> The entity reference type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() )}.
	 * @param <E> The entity type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() )}.
	 * @param <LOS> The type of the initial step of the loading options definition DSL.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() )}.
	 * @return The extension.
	 */
	@SuppressWarnings("unchecked") // The instance works for any H, R, E and LOS
	public static <H, R, E, LOS> ElasticsearchExtension<H, R, E, LOS> get() {
		return (ElasticsearchExtension<H, R, E, LOS>) INSTANCE;
	}

	private ElasticsearchExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchQuerySelectStep<R, E, LOS>> extendOptional(
			SearchQuerySelectStep<?, R, E, LOS, ?, ?> original,
			SearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		if ( scope instanceof ElasticsearchSearchQueryIndexScope ) {
			return Optional.of( new ElasticsearchSearchQuerySelectStepImpl<>(
					(ElasticsearchSearchQueryIndexScope<?>) scope, sessionContext, loadingContextBuilder
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
			SearchLoadingContext<?> loadingContext) {
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
	public Optional<ElasticsearchSearchPredicateFactory> extendOptional(SearchPredicateFactory original) {
		if ( original instanceof ElasticsearchSearchPredicateFactory ) {
			return Optional.of( (ElasticsearchSearchPredicateFactory) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchSortFactory> extendOptional(
			SearchSortFactory original) {
		if ( original instanceof ElasticsearchSearchSortFactory ) {
			return Optional.of( (ElasticsearchSearchSortFactory) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchProjectionFactory<R, E>> extendOptional(SearchProjectionFactory<R, E> original) {
		if ( original instanceof ElasticsearchSearchProjectionFactory ) {
			return Optional.of( (ElasticsearchSearchProjectionFactory<R, E>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchAggregationFactory> extendOptional(
			SearchAggregationFactory original) {
		if ( original instanceof ElasticsearchSearchAggregationFactory ) {
			return Optional.of( (ElasticsearchSearchAggregationFactory) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ElasticsearchIndexFieldTypeFactory extendOrFail(IndexFieldTypeFactory original) {
		if ( original instanceof ElasticsearchIndexFieldTypeFactory ) {
			return (ElasticsearchIndexFieldTypeFactory) original;
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( original );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ElasticsearchIndexSchemaExport extendOrFail(SchemaExport original) {
		if ( original instanceof ElasticsearchIndexSchemaExport ) {
			return ( (ElasticsearchIndexSchemaExport) original );
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( original );
		}
	}
}
