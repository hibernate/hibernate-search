/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene;

import java.util.Optional;

import org.hibernate.search.backend.lucene.logging.impl.LuceneMiscLog;
import org.hibernate.search.backend.lucene.schema.management.LuceneIndexSchemaExport;
import org.hibernate.search.backend.lucene.scope.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQuerySelectStep;
import org.hibernate.search.backend.lucene.search.query.dsl.impl.LuceneSearchQuerySelectStepImpl;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactory;
import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
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

/**
 * An extension for the Lucene backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @param <H> The type of query hits.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() )}.
 * @param <R> The entity reference type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() )}.
 * @param <E> entity type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() )}.
 * @param <LOS> The type of the initial step of the loading options definition DSL.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() )}.
 *
 * @see #get()
 */
public final class LuceneExtension<SR, H, R, E, LOS>
		implements SearchQueryDslExtension<SR, LuceneSearchQuerySelectStep<SR, R, E, LOS>, R, E, LOS>,
		SearchQueryExtension<LuceneSearchQuery<H>, H>,
		SearchPredicateFactoryExtension<LuceneSearchPredicateFactory<SR>>,
		SearchSortFactoryExtension<LuceneSearchSortFactory<SR>>,
		SearchProjectionFactoryExtension<LuceneSearchProjectionFactory<SR, R, E>, R, E>,
		SearchAggregationFactoryExtension<LuceneSearchAggregationFactory<SR>>,
		IndexFieldTypeFactoryExtension<LuceneIndexFieldTypeFactory>,
		IndexScopeExtension<LuceneIndexScope>,
		SchemaExportExtension<LuceneIndexSchemaExport> {

	private static final LuceneExtension<Object, Object, Object, Object, Object> INSTANCE = new LuceneExtension<>();

	/**
	 * Get the extension with generic parameters automatically set as appropriate for the context in which it's used.
	 *
	 * @param <SR> Scope root type.
	 * @param <H> The type of query hits.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @param <R> The entity reference type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @param <E> entity type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @param <LOS> The type of the initial step of the loading options definition DSL.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @return The extension.
	 */
	@SuppressWarnings("unchecked") // The instance works for any H, R and E
	public static <SR, H, R, E, LOS> LuceneExtension<SR, H, R, E, LOS> get() {
		return (LuceneExtension<SR, H, R, E, LOS>) INSTANCE;
	}

	private LuceneExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<LuceneSearchQuerySelectStep<SR, R, E, LOS>> extendOptional(
			SearchQuerySelectStep<SR, ?, R, E, LOS, ?, ?> original,
			SearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		if ( scope instanceof LuceneSearchQueryIndexScope ) {
			return Optional.of( new LuceneSearchQuerySelectStepImpl<>(
					(LuceneSearchQueryIndexScope<?>) scope, sessionContext, loadingContextBuilder
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
	public Optional<LuceneSearchQuery<H>> extendOptional(SearchQuery<H> original,
			SearchLoadingContext<?> loadingContext) {
		if ( original instanceof LuceneSearchQuery ) {
			return Optional.of( (LuceneSearchQuery<H>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Optional<LuceneSearchPredicateFactory<SR>> extendOptional(SearchPredicateFactory original) {
		if ( original instanceof LuceneSearchPredicateFactory<?> f ) {
			return Optional.of( (LuceneSearchPredicateFactory<SR>) f );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Optional<LuceneSearchSortFactory<SR>> extendOptional(SearchSortFactory original) {
		if ( original instanceof LuceneSearchSortFactory ) {
			return Optional.of( (LuceneSearchSortFactory<SR>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Optional<LuceneSearchProjectionFactory<SR, R, E>> extendOptional(SearchProjectionFactory<R, E> original) {
		if ( original instanceof LuceneSearchProjectionFactory ) {
			return Optional.of( (LuceneSearchProjectionFactory<SR, R, E>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Optional<LuceneSearchAggregationFactory<SR>> extendOptional(SearchAggregationFactory original) {
		if ( original instanceof LuceneSearchAggregationFactory ) {
			return Optional.of( (LuceneSearchAggregationFactory<SR>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LuceneIndexFieldTypeFactory extendOrFail(IndexFieldTypeFactory original) {
		if ( original instanceof LuceneIndexFieldTypeFactory ) {
			return (LuceneIndexFieldTypeFactory) original;
		}
		else {
			throw LuceneMiscLog.INSTANCE.luceneExtensionOnUnknownType( original );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LuceneIndexScope extendOrFail(IndexScope original) {
		if ( original instanceof LuceneIndexScope ) {
			return (LuceneIndexScope) original;
		}
		else {
			throw LuceneMiscLog.INSTANCE.luceneExtensionOnUnknownType( original );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LuceneIndexSchemaExport extendOrFail(SchemaExport original) {
		if ( original instanceof LuceneIndexSchemaExport ) {
			return (LuceneIndexSchemaExport) original;
		}
		else {
			throw LuceneMiscLog.INSTANCE.luceneExtensionOnUnknownType( original );
		}
	}
}
