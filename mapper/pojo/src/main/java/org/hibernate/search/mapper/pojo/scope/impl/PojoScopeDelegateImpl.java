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

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.mapper.pojo.work.impl.PojoScopeWorkExecutorImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoScopeDelegateImpl<R, E, E2, C> implements PojoScopeDelegate<R, E2, C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <R, E, E2, C> PojoScopeDelegate<R, E2, C> create(
			PojoScopeIndexedTypeContextProvider indexedTypeContextProvider,
			PojoScopeContainedTypeContextProvider containedTypeContextProvider,
			Collection<? extends Class<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider,
			AbstractPojoSessionContextImplementor sessionContext) {
		if ( targetedTypes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}

		Set<PojoScopeIndexedTypeContext<?, ? extends E, ?>> targetedTypeContexts = new LinkedHashSet<>();
		Set<Class<?>> nonIndexedTypes = new LinkedHashSet<>();
		Set<Class<?>> nonIndexedButContainedTypes = new LinkedHashSet<>();
		for ( Class<? extends E> targetedType : targetedTypes ) {
			Optional<? extends Set<? extends PojoScopeIndexedTypeContext<?, ? extends E, ?>>> targetedTypeManagersForType =
					indexedTypeContextProvider.getAllBySuperClass( targetedType );
			if ( targetedTypeManagersForType.isPresent() ) {
				targetedTypeContexts.addAll( targetedTypeManagersForType.get() );
			}
			else {
				// Remember this to produce a clear error message
				nonIndexedTypes.add( targetedType );
				if ( containedTypeContextProvider.getByExactClass( targetedType ).isPresent() ) {
					nonIndexedButContainedTypes.add( targetedType );
				}
			}
		}

		if ( !nonIndexedTypes.isEmpty() || !nonIndexedButContainedTypes.isEmpty() ) {
			throw log.invalidScopeTarget( nonIndexedTypes, nonIndexedButContainedTypes );
		}

		Set<C> targetedTypeExtendedContexts =
				targetedTypeContexts.stream()
						.map( PojoScopeIndexedTypeContext::getJavaClass )
						.map( indexedTypeExtendedContextProvider::getByExactClass )
						.collect( Collectors.toCollection( LinkedHashSet::new ) );

		return new PojoScopeDelegateImpl<>(
				targetedTypeContexts, targetedTypeExtendedContexts,
				sessionContext
		);
	}

	private final Set<? extends PojoScopeIndexedTypeContext<?, ? extends E, ?>> targetedTypeContexts;
	private final Set<C> targetedTypeExtendedContexts;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private MappedIndexScope<R, E2> delegate;
	private PojoScopeWorkExecutor executor;

	private PojoScopeDelegateImpl(Set<? extends PojoScopeIndexedTypeContext<?, ? extends E, ?>> targetedTypeContexts,
			Set<C> targetedTypeExtendedContexts,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.targetedTypeContexts = targetedTypeContexts;
		this.targetedTypeExtendedContexts = targetedTypeExtendedContexts;
		this.sessionContext = sessionContext;
	}

	@Override
	public Set<C> getIncludedIndexedTypes() {
		return targetedTypeExtendedContexts;
	}

	@Override
	public SearchQueryHitTypeStep<?, R, E2, SearchProjectionFactory<R, E2>, ?> search(
			LoadingContextBuilder<R, E2> loadingContextBuilder) {
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
	public PojoScopeWorkExecutor executor() {
		if ( executor == null ) {
			executor = new PojoScopeWorkExecutorImpl(
					targetedTypeContexts, DetachedSessionContextImplementor.of( sessionContext )
			);
		}
		return executor;
	}

	private MappedIndexScope<R, E2> getIndexScope() {
		AbstractPojoMappingContextImplementor mappingContext = sessionContext.getMappingContext();
		if ( delegate == null ) {
			Iterator<? extends PojoScopeIndexedTypeContext<?, ? extends E, ?>> iterator = targetedTypeContexts.iterator();
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
