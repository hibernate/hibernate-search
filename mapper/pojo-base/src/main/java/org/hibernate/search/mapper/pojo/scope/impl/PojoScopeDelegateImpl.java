/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoDefaultMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
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
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoScopeDelegateImpl<R, E, C> implements PojoScopeDelegate<R, E, C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <R, E, C> PojoScopeDelegate<R, E, C> create(
			PojoScopeMappingContext mappingContext,
			PojoScopeTypeContextProvider typeContextProvider,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		if ( targetedTypes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}

		Set<PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts = new LinkedHashSet<>();
		Set<PojoRawTypeIdentifier<?>> nonIndexedTypes = new LinkedHashSet<>();
		for ( PojoRawTypeIdentifier<? extends E> targetedType : targetedTypes ) {
			Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> targetedTypeManagersForType =
					typeContextProvider.allIndexedForSuperType( targetedType );
			if ( targetedTypeManagersForType.isPresent() ) {
				targetedTypeContexts.addAll( targetedTypeManagersForType.get() );
			}
			else {
				// Remember this to produce a clear error message
				nonIndexedTypes.add( targetedType );
			}
		}

		if ( !nonIndexedTypes.isEmpty() ) {
			throw log.invalidScopeTarget( nonIndexedTypes, typeContextProvider.allIndexedSuperTypes() );
		}

		Set<C> targetedTypeExtendedContexts =
				targetedTypeContexts.stream()
						.map( PojoScopeIndexedTypeContext::typeIdentifier )
						.map( indexedTypeExtendedContextProvider::forExactType )
						.collect( Collectors.toCollection( LinkedHashSet::new ) );

		return new PojoScopeDelegateImpl<>(
				mappingContext, typeContextProvider,
				targetedTypeContexts, targetedTypeExtendedContexts
		);
	}

	private final PojoScopeMappingContext mappingContext;
	private final PojoScopeTypeContextProvider indexedTypeContextProvider;
	private final Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts;
	private final Set<C> targetedTypeExtendedContexts;
	private MappedIndexScope<R, E> delegate;

	private PojoScopeDelegateImpl(PojoScopeMappingContext mappingContext,
			PojoScopeTypeContextProvider indexedTypeContextProvider,
			Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts,
			Set<C> targetedTypeExtendedContexts) {
		this.mappingContext = mappingContext;
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.targetedTypeContexts = targetedTypeContexts;
		this.targetedTypeExtendedContexts = Collections.unmodifiableSet( targetedTypeExtendedContexts );
	}

	@Override
	public Set<C> includedIndexedTypes() {
		return targetedTypeExtendedContexts;
	}

	@Override
	public <LOS> SearchQuerySelectStep<?, R, E, LOS, SearchProjectionFactory<R, E>, ?> search(
			PojoScopeSessionContext sessionContext, DocumentReferenceConverter<R> documentReferenceConverter,
			PojoSelectionLoadingContextBuilder<LOS> loadingContextBuilder) {
		Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName = new LinkedHashMap<>();
		for ( PojoScopeIndexedTypeContext<?, ? extends E> type : targetedTypeContexts ) {
			targetTypesByEntityName.put( type.entityName(), type );
		}
		return getIndexScope().search( sessionContext, new PojoSearchLoadingContextBuilder<>(
				targetTypesByEntityName, documentReferenceConverter, sessionContext, loadingContextBuilder ) );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return getIndexScope().predicate();
	}

	@Override
	public SearchSortFactory sort() {
		return getIndexScope().sort();
	}

	@Override
	public SearchProjectionFactory<R, E> projection() {
		return getIndexScope().projection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return getIndexScope().aggregation();
	}

	@Override
	public PojoScopeWorkspace workspace(DetachedBackendSessionContext sessionContext) {
		return new PojoScopeWorkspaceImpl(
				targetedTypeContexts, sessionContext
		);
	}

	@Override
	public PojoScopeSchemaManager schemaManager() {
		return new PojoScopeSchemaManagerImpl( targetedTypeContexts );
	}

	@Override
	public PojoMassIndexer massIndexer(PojoMassIndexingContext context, DetachedBackendSessionContext detachedSession) {
		return new PojoDefaultMassIndexer( context, mappingContext, indexedTypeContextProvider, targetedTypeContexts,
				schemaManager(), detachedSession, workspace( detachedSession ) );
	}

	@Override
	public <T> T extension(IndexScopeExtension<T> extension) {
		return getIndexScope().extension( extension );
	}

	private MappedIndexScope<R, E> getIndexScope() {
		if ( delegate == null ) {
			Iterator<? extends PojoScopeIndexedTypeContext<?, ? extends E>> iterator = targetedTypeContexts.iterator();
			MappedIndexScopeBuilder<R, E> builder = iterator.next().createScopeBuilder( mappingContext );
			while ( iterator.hasNext() ) {
				iterator.next().addTo( builder );
			}
			delegate = builder.build();
		}
		return delegate;
	}
}
