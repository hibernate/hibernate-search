/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.schema.management.impl.PojoScopeSchemaManagerImpl;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.pojo.work.impl.PojoScopeWorkspaceImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoScopeDelegateImpl<R, E, E2, C> implements PojoScopeDelegate<R, E2, C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <R, E, E2, C> PojoScopeDelegate<R, E2, C> create(
			PojoScopeMappingContext mappingContext,
			PojoScopeIndexedTypeContextProvider indexedTypeContextProvider,
			PojoScopeContainedTypeContextProvider containedTypeContextProvider,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		if ( targetedTypes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}

		Set<PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts = new LinkedHashSet<>();
		Set<PojoRawTypeIdentifier<?>> nonIndexedTypes = new LinkedHashSet<>();
		Set<PojoRawTypeIdentifier<?>> nonIndexedButContainedTypes = new LinkedHashSet<>();
		for ( PojoRawTypeIdentifier<? extends E> targetedType : targetedTypes ) {
			Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>>> targetedTypeManagersForType =
					indexedTypeContextProvider.getAllBySuperType( targetedType );
			if ( targetedTypeManagersForType.isPresent() ) {
				targetedTypeContexts.addAll( targetedTypeManagersForType.get() );
			}
			else {
				// Remember this to produce a clear error message
				nonIndexedTypes.add( targetedType );
				if ( containedTypeContextProvider.getByExactType( targetedType ).isPresent() ) {
					nonIndexedButContainedTypes.add( targetedType );
				}
			}
		}

		if ( !nonIndexedTypes.isEmpty() || !nonIndexedButContainedTypes.isEmpty() ) {
			throw log.invalidScopeTarget( nonIndexedTypes, nonIndexedButContainedTypes );
		}

		Set<C> targetedTypeExtendedContexts =
				targetedTypeContexts.stream()
						.map( PojoScopeIndexedTypeContext::getTypeIdentifier )
						.map( indexedTypeExtendedContextProvider::forExactType )
						.collect( Collectors.toCollection( LinkedHashSet::new ) );

		return new PojoScopeDelegateImpl<>(
				mappingContext,
				targetedTypeContexts, targetedTypeExtendedContexts
		);
	}

	private final PojoScopeMappingContext mappingContext;
	private final Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts;
	private final Set<C> targetedTypeExtendedContexts;
	private MappedIndexScope<R, E2> delegate;

	private PojoScopeDelegateImpl(PojoScopeMappingContext mappingContext,
			Set<? extends PojoScopeIndexedTypeContext<?, ? extends E>> targetedTypeContexts,
			Set<C> targetedTypeExtendedContexts) {
		this.mappingContext = mappingContext;
		this.targetedTypeContexts = targetedTypeContexts;
		this.targetedTypeExtendedContexts = targetedTypeExtendedContexts;
	}

	@Override
	public Set<C> includedIndexedTypes() {
		return targetedTypeExtendedContexts;
	}

	@Override
	public <LOS> SearchQuerySelectStep<?, R, E2, LOS, SearchProjectionFactory<R, E2>, ?> search(
			BackendSessionContext sessionContext,
			LoadingContextBuilder<R, E2, LOS> loadingContextBuilder) {
		return getIndexScope().search( sessionContext, loadingContextBuilder );
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
	public SearchProjectionFactory<R, E2> projection() {
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

	private MappedIndexScope<R, E2> getIndexScope() {
		if ( delegate == null ) {
			Iterator<? extends PojoScopeIndexedTypeContext<?, ? extends E>> iterator = targetedTypeContexts.iterator();
			MappedIndexScopeBuilder<R, E2> builder = iterator.next().createScopeBuilder(
					mappingContext
			);
			while ( iterator.hasNext() ) {
				iterator.next().addTo( builder );
			}
			delegate = builder.build();
		}
		return delegate;
	}
}
