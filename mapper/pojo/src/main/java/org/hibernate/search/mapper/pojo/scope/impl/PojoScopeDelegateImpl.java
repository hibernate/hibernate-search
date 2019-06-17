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

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoScopeWorkExecutorImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.StreamHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoScopeDelegateImpl<E, E2> implements PojoScopeDelegate<E, E2> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <E, E2> PojoScopeDelegate<E, E2> create(
			PojoScopeIndexedTypeContextProvider indexedTypeContextProvider,
			PojoScopeContainedTypeContextProvider containedTypeContextProvider,
			Collection<? extends Class<? extends E>> targetedTypes,
			AbstractPojoSessionContextImplementor sessionContext) {
		if ( targetedTypes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}

		Set<PojoScopeIndexedTypeContextImplementor<?, ? extends E, ?>> targetedTypeContexts = new LinkedHashSet<>();
		Set<Class<?>> nonIndexedTypes = new LinkedHashSet<>();
		Set<Class<?>> nonIndexedButContainedTypes = new LinkedHashSet<>();
		for ( Class<? extends E> targetedType : targetedTypes ) {
			Optional<? extends Set<? extends PojoScopeIndexedTypeContextImplementor<?, ? extends E, ?>>> targetedTypeManagersForType =
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

		return new PojoScopeDelegateImpl<>( indexedTypeContextProvider, targetedTypeContexts, sessionContext );
	}

	private final PojoScopeIndexedTypeContextProvider typeContextProvider;
	private final Set<? extends PojoScopeIndexedTypeContextImplementor<?, ? extends E, ?>> targetedTypeContexts;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private MappedIndexScope<PojoReference, E2> delegate;
	private PojoScopeWorkExecutor executor;

	private PojoScopeDelegateImpl(PojoScopeIndexedTypeContextProvider typeContextProvider,
			Set<? extends PojoScopeIndexedTypeContextImplementor<?, ? extends E, ?>> targetedTypeContexts,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.typeContextProvider = typeContextProvider;
		this.targetedTypeContexts = Collections.unmodifiableSet( targetedTypeContexts );
		this.sessionContext = sessionContext;
	}

	@Override
	public Set<? extends PojoScopeIndexedTypeContext<? extends E>> getIncludedIndexedTypes() {
		return targetedTypeContexts;
	}

	@Override
	public PojoReference toPojoReference(DocumentReference documentReference) {
		PojoScopeIndexedTypeContextImplementor<?, ?, ?> typeContext = typeContextProvider.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference"
				) );
		Object id = typeContext.getIdentifierMapping().fromDocumentIdentifier( documentReference.getId(), sessionContext );
		return new PojoReferenceImpl( typeContext.getJavaClass(), id );
	}

	@Override
	public SearchQueryResultDefinitionContext<?, PojoReference, E2, SearchProjectionFactoryContext<PojoReference, E2>, ?> search(
			LoadingContextBuilder<PojoReference, E2> loadingContextBuilder) {
		return getIndexScope().search( sessionContext, loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return getIndexScope().predicate();
	}

	@Override
	public SearchSortFactoryContext sort() {
		return getIndexScope().sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, E2> projection() {
		return getIndexScope().projection();
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

	private MappedIndexScope<PojoReference, E2> getIndexScope() {
		AbstractPojoMappingContextImplementor mappingContext = sessionContext.getMappingContext();
		if ( delegate == null ) {
			Iterator<? extends PojoScopeIndexedTypeContextImplementor<?, ? extends E, ?>> iterator = targetedTypeContexts.iterator();
			MappedIndexScopeBuilder<PojoReference, E2> builder = iterator.next().createScopeBuilder(
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
