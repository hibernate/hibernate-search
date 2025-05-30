/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoDefaultMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.schema.management.impl.PojoScopeSchemaManagerImpl;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.search.loading.impl.PojoSearchLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.search.loading.impl.PojoSearchLoadingIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoScopeWorkspaceImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

public final class PojoScopeDelegateImpl<SR, R extends EntityReference, E, C> implements PojoScopeDelegate<SR, R, E, C> {

	public static <SR, R extends EntityReference, E, C> PojoScopeDelegate<SR, R, E, C> create(
			PojoScopeMappingContext mappingContext,
			Class<SR> rootScope,
			PojoScopeTypeContextProvider typeContextProvider,
			Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		Set<C> targetedTypeExtendedContexts =
				targetedTypeContexts.stream()
						.map( PojoScopeIndexedTypeContext::typeIdentifier )
						.map( indexedTypeExtendedContextProvider::forExactType )
						.collect( Collectors.toCollection( LinkedHashSet::new ) );

		return new PojoScopeDelegateImpl<>(
				mappingContext, rootScope, typeContextProvider,
				targetedTypeContexts, targetedTypeExtendedContexts
		);
	}

	private final PojoScopeMappingContext mappingContext;
	private final Class<SR> rootScope;
	private final PojoScopeTypeContextProvider indexedTypeContextProvider;
	private final Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts;
	private final Set<C> targetedTypeExtendedContexts;
	private MappedIndexScope<SR, R, E> delegate;

	private PojoScopeDelegateImpl(PojoScopeMappingContext mappingContext,
			Class<SR> rootScope,
			PojoScopeTypeContextProvider indexedTypeContextProvider,
			Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts,
			Set<C> targetedTypeExtendedContexts) {
		this.mappingContext = mappingContext;
		this.rootScope = rootScope;
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.targetedTypeContexts = targetedTypeContexts;
		this.targetedTypeExtendedContexts = Collections.unmodifiableSet( targetedTypeExtendedContexts );
	}

	@Override
	public Set<C> includedIndexedTypes() {
		return targetedTypeExtendedContexts;
	}

	@Override
	public <LOS> SearchQuerySelectStep<SR, ?, R, E, LOS, TypedSearchProjectionFactory<SR, R, E>, ?> search(
			PojoScopeSessionContext sessionContext,
			PojoSelectionLoadingContextBuilder<LOS> loadingContextBuilder) {
		Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName = new LinkedHashMap<>();
		for ( PojoScopeIndexedTypeContext<?, ? extends E> type : targetedTypeContexts ) {
			targetTypesByEntityName.put( type.entityName(), type );
		}
		return getIndexScope().search( sessionContext, new PojoSearchLoadingContextBuilder<>(
				targetTypesByEntityName, sessionContext.mappingContext().entityReferenceFactoryDelegate(),
				sessionContext, loadingContextBuilder ) );
	}

	@Override
	public TypedSearchPredicateFactory<SR> predicate() {
		return getIndexScope().predicate();
	}

	@Override
	public TypedSearchSortFactory<SR> sort() {
		return getIndexScope().sort();
	}

	@Override
	public TypedSearchProjectionFactory<SR, R, E> projection() {
		return getIndexScope().projection();
	}

	@Override
	public TypedSearchAggregationFactory<SR> aggregation() {
		return getIndexScope().aggregation();
	}

	@Override
	public SearchHighlighterFactory highlighter() {
		return getIndexScope().highlighter();
	}

	@Override
	public PojoScopeWorkspace workspace(String tenantId) {
		return new PojoScopeWorkspaceImpl( mappingContext, targetedTypeContexts, asSetIgnoreNull( tenantId ) );
	}

	@Override
	public PojoScopeWorkspace workspace(Set<String> tenantIds) {
		return new PojoScopeWorkspaceImpl( mappingContext, targetedTypeContexts, tenantIds );
	}

	@Override
	public PojoScopeSchemaManager schemaManager() {
		return new PojoScopeSchemaManagerImpl( targetedTypeContexts );
	}

	@Override
	public PojoMassIndexer massIndexer(PojoMassIndexingContext context) {
		return new PojoDefaultMassIndexer( context, mappingContext, indexedTypeContextProvider, targetedTypeContexts,
				schemaManager(), this );
	}

	@Override
	public <T> T extension(IndexScopeExtension<T> extension) {
		return getIndexScope().extension( extension );
	}

	private MappedIndexScope<SR, R, E> getIndexScope() {
		if ( delegate == null ) {
			Iterator<? extends PojoScopeIndexedTypeContext<?, ? extends E>> iterator = targetedTypeContexts.iterator();
			MappedIndexScopeBuilder<SR, R, E> builder = iterator.next().createScopeBuilder( mappingContext, rootScope );
			while ( iterator.hasNext() ) {
				iterator.next().addTo( builder );
			}
			delegate = builder.build();
		}
		return delegate;
	}
}
