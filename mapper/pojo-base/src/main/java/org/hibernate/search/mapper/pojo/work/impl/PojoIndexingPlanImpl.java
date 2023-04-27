/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingAssociationInverseSideCollector;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.loading.impl.PojoLoadingPlan;
import org.hibernate.search.mapper.pojo.loading.impl.PojoMultiLoaderLoadingPlan;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoIndexingPlanImpl
		implements PojoIndexingPlan, PojoLoadingPlanProvider,
				PojoReindexingCollector, PojoReindexingAssociationInverseSideCollector,
				PojoIndexingProcessorRootContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkTypeContextProvider typeContextProvider;
	private final PojoWorkSessionContext sessionContext;
	private final PojoRuntimeIntrospector introspector;
	private final PojoIndexingPlanStrategy strategy;

	// Use a LinkedHashMap for deterministic iteration
	protected final Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeIndexingPlan<?, ?>> indexedTypeDelegates = new LinkedHashMap<>();
	protected final Map<PojoRawTypeIdentifier<?>, PojoContainedTypeIndexingPlan<?, ?>> containedTypeDelegates = new LinkedHashMap<>();

	private boolean isProcessing = false;
	private PojoLoadingPlan<Object> loadingPlan = null;

	public PojoIndexingPlanImpl(PojoWorkTypeContextProvider typeContextProvider,
			PojoWorkSessionContext sessionContext,
			PojoIndexingPlanStrategy strategy) {
		this.typeContextProvider = typeContextProvider;
		this.sessionContext = sessionContext;
		this.introspector = sessionContext.runtimeIntrospector();
		this.strategy = strategy;
	}

	@Override
	@Deprecated
	public void add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.add( providedId, providedRoutes, entity );
	}

	@Override
	@Deprecated
	public void addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.addOrUpdate( providedId, providedRoutes, entity, forceSelfDirty, forceContainingDirty, dirtyPaths );
	}

	@Override
	@Deprecated
	public void delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes,
			Object entity) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.delete( providedId, providedRoutes, entity );
	}

	@Override
	@Deprecated
	public void addOrUpdateOrDelete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, boolean forceSelfDirty, boolean forceContainingDirty,
			BitSet dirtyPaths) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.addOrUpdateOrDelete( providedId, providedRoutes, forceSelfDirty, forceContainingDirty, dirtyPaths );
	}

	@Override
	@Deprecated
	public void updateAssociationInverseSide(PojoRawTypeIdentifier<?> typeIdentifier,
			BitSet dirtyAssociationPaths, Object[] oldState, Object[] newState) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.updateAssociationInverseSide( dirtyAssociationPaths, oldState, newState );
	}

	@Override
	public AbstractPojoTypeIndexingPlan<?, ?, ?> typeIfIncludedOrNull(PojoRawTypeIdentifier<?> typeIdentifier) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeOrNull( typeIdentifier );
		if ( delegate == null && sessionContext.configuredIndexingPlanFilter().isIncluded( typeIdentifier ) ) {
			delegate = createDelegate( typeIdentifier );
		}
		return delegate;
	}

	private AbstractPojoTypeIndexingPlan<?, ?, ?> typeOrNull(PojoRawTypeIdentifier<?> typeIdentifier) {
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = indexedTypeDelegates.get( typeIdentifier );
		if ( delegate == null ) {
			delegate = containedTypeDelegates.get( typeIdentifier );
		}
		return delegate;
	}

	@Override
	public void process() {
		if ( isProcessing ) {
			throw log.recursiveIndexingPlanProcess();
		}

		isProcessing = true;
		try {
			for ( PojoContainedTypeIndexingPlan<?, ?> delegate : containedTypeDelegates.values() ) {
				delegate.planLoading();
			}
			for ( PojoIndexedTypeIndexingPlan<?, ?> delegate : indexedTypeDelegates.values() ) {
				delegate.planLoading();
			}
			if ( loadingPlan != null ) {
				loadingPlan.loadBlocking( null );
			}
			boolean shouldResolveDirtyForDeleteOnly = strategy.shouldResolveDirtyForDeleteOnly();
			for ( PojoContainedTypeIndexingPlan<?, ?> delegate : containedTypeDelegates.values() ) {
				delegate.resolveDirty( shouldResolveDirtyForDeleteOnly );
			}
			// We need to iterate on a "frozen snapshot" of the indexedTypeDelegates values because of HSEARCH-3857
			List<PojoIndexedTypeIndexingPlan<?, ?>> frozenIndexedTypeDelegates =
					new ArrayList<>( indexedTypeDelegates.values() );
			for ( PojoIndexedTypeIndexingPlan<?, ?> delegate : frozenIndexedTypeDelegates ) {
				delegate.resolveDirty( shouldResolveDirtyForDeleteOnly );
			}
			for ( PojoContainedTypeIndexingPlan<?, ?> delegate : containedTypeDelegates.values() ) {
				delegate.process( this );
			}
			for ( PojoIndexedTypeIndexingPlan<?, ?> delegate : indexedTypeDelegates.values() ) {
				delegate.process( this );
			}
		}
		finally {
			isProcessing = false;
			loadingPlan = null;
			clearStates();
		}
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter) {
		try {
			process();
			return strategy.doExecuteAndReport(
					indexedTypeDelegates.values(),
					this,
					operationSubmitter
			);
		}
		finally {
			indexedTypeDelegates.clear();
		}
	}

	@Override
	public void discard() {
		try {
			strategy.doDiscard( indexedTypeDelegates.values() );
		}
		finally {
			indexedTypeDelegates.clear();
			containedTypeDelegates.clear();
		}
	}

	@Override
	public void discardNotProcessed() {
		clearStates();
	}

	private void clearStates() {
		for ( PojoIndexedTypeIndexingPlan<?, ?> delegate : indexedTypeDelegates.values() ) {
			delegate.clearStates();
		}
		for ( PojoContainedTypeIndexingPlan<?, ?> delegate : containedTypeDelegates.values() ) {
			delegate.clearStates();
		}
	}

	@Override
	public void updateBecauseOfContained(PojoRawTypeIdentifier<?> typeIdentifier, Object containingEntity) {
		// Note this method won't work when using provided identifiers
		// Fortunately, all platforms relying on provided identifiers (Infinispan)
		// also disable reindexing of other entities on updates,
		// so they won't ever call this method.

		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.updateBecauseOfContained( containingEntity );
	}

	@Override
	public void updateBecauseOfContainedAssociation(PojoRawTypeIdentifier<?> typeIdentifier, Object containingEntity,
			int dirtyAssociationPathOrdinal) {
		// Note this method won't work when using provided identifiers
		// or on contained entities that do not define a identifier mapping.
		// Fortunately, the only platform making use of this method (Hibernate ORM)
		// never uses provided identifiers and always defines an identifier mapping,
		// so this should always work.

		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeIfIncludedOrNull( typeIdentifier );
		if ( delegate == null ) {
			return;
		}
		delegate.updateBecauseOfContainedAssociation( containingEntity, dirtyAssociationPathOrdinal );
	}

	@Override
	public PojoIndexingProcessorSessionContext sessionContext() {
		return sessionContext;
	}

	@Override
	public boolean isDeleted(Object unproxiedObject) {
		PojoRawTypeIdentifier<?> typeIdentifier = introspector.detectEntityType( unproxiedObject );
		if ( typeIdentifier == null ) {
			// Not a type that can be marked as deleted in this indexing plan.
			return false;
		}
		AbstractPojoTypeIndexingPlan<?, ?, ?> delegate = typeOrNull( typeIdentifier );
		if ( delegate == null ) {
			// No event whatsoever for that type, so definitely no delete event.
			return false;
		}
		return delegate.isDeleted( unproxiedObject );
	}

	private AbstractPojoTypeIndexingPlan<?, ?, ?> createDelegate(PojoRawTypeIdentifier<?> typeIdentifier) {
		if ( !sessionContext.configuredIndexingPlanFilter().isIncluded( typeIdentifier ) ) {
			throw log.attemptToCreateIndexingPlanForExcludedType( typeIdentifier );
		}
		PojoWorkTypeContext<?, ?> typeContext = typeContextProvider.forExactType( typeIdentifier );
		Optional<? extends PojoWorkIndexedTypeContext<?, ?>> indexedTypeContextOptional =
				typeContext.asIndexed();
		if ( indexedTypeContextOptional.isPresent() ) {
			// extracting a variable to work around an Eclipse compiler issue
			PojoWorkIndexedTypeContext<?, ?> indexedTypeContext = indexedTypeContextOptional.get();
			PojoIndexedTypeIndexingPlan<?, ?> delegate = createDelegate( indexedTypeContext );
			indexedTypeDelegates.put( typeIdentifier, delegate );
			return delegate;
		}
		else {
			// extracting a variable to work around an Eclipse compiler issue
			PojoWorkContainedTypeContext<?, ?> containedTypeContext = typeContext.asContained()
					.orElseThrow( () -> new AssertionFailure( "Type is neither indexed nor contained" ) );
			PojoContainedTypeIndexingPlan<?, ?> delegate = createDelegate( containedTypeContext );
			containedTypeDelegates.put( typeIdentifier, delegate );
			return delegate;
		}
	}

	@Override
	public PojoLoadingPlan<Object> loadingPlan() {
		if ( loadingPlan == null ) {
			loadingPlan = new PojoMultiLoaderLoadingPlan<>( sessionContext.defaultLoadingContext() );
		}
		return loadingPlan;
	}

	private <I, E> PojoIndexedTypeIndexingPlan<I, E> createDelegate(PojoWorkIndexedTypeContext<I, E> typeContext) {
		return strategy.createIndexedDelegate( typeContext, sessionContext, this );
	}

	private PojoContainedTypeIndexingPlan<?, ?> createDelegate(PojoWorkContainedTypeContext<?, ?> typeContext) {
		return strategy.createDelegate( typeContext, sessionContext, this );
	}
}
