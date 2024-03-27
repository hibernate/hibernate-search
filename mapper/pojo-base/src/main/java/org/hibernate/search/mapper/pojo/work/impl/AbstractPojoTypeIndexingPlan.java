/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingAssociationInverseSideResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.spi.PojoImplicitReindexingResolverSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoTypeIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 * @param <S> The type of per-instance state.
 */
abstract class AbstractPojoTypeIndexingPlan<I, E, S extends AbstractPojoTypeIndexingPlan<I, E, S>.AbstractEntityState>
		implements PojoImplicitReindexingAssociationInverseSideResolverRootContext, PojoTypeIndexingPlan {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final PojoWorkSessionContext sessionContext;
	final PojoIndexingPlanImpl root;
	final PojoTypeIndexingPlanDelegate<I, E> delegate;

	// Use a LinkedHashMap for deterministic iteration
	final Map<I, S> statesPerId = new LinkedHashMap<>();
	private boolean mayRequireLoading = false;

	AbstractPojoTypeIndexingPlan(PojoWorkSessionContext sessionContext,
			PojoIndexingPlanImpl root,
			PojoTypeIndexingPlanDelegate<I, E> delegate) {
		this.sessionContext = sessionContext;
		this.root = root;
		this.delegate = delegate;
	}

	@Override
	public void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		if ( !mayRequireLoading && entity == null ) {
			mayRequireLoading = true;
		}
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.add( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	@Override
	public void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths) {
		if ( !mayRequireLoading && entity == null ) {
			mayRequireLoading = true;
		}
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.addOrUpdate( entitySupplier, dirtyPaths, forceSelfDirty, forceContainingDirty );
		state.providedRoutes( providedRoutes );
	}

	@Override
	public void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.delete( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	@Override
	public void addOrUpdateOrDelete(Object providedId, DocumentRoutesDescriptor providedRoutes, boolean forceSelfDirty,
			boolean forceContainingDirty, BitSet dirtyPaths) {
		if ( !mayRequireLoading ) {
			mayRequireLoading = true;
		}
		I identifier = toIdentifier( providedId, null );
		S state = getState( identifier );
		state.addOrUpdateOrDelete( dirtyPaths, forceSelfDirty, forceContainingDirty );
		state.providedRoutes( providedRoutes );
	}

	@Override
	public void updateAssociationInverseSide(BitSet dirtyAssociationPaths, Object[] oldState, Object[] newState) {
		typeContext().reindexingResolver().associationInverseSideResolver()
				.resolveEntitiesToReindex( root, dirtyAssociationPaths, oldState, newState, this );
	}

	// Should only be called on indexed types,
	// but it's simpler to implement this method for both indexed and contained types.

	void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = typeContext().identifierMapping().getIdentifier( null, entitySupplier );
		getState( identifier ).updateBecauseOfContained( entitySupplier );
	}

	void updateBecauseOfContainedAssociation(Object entity, int dirtyAssociationPathOrdinal) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = typeContext().identifierMapping().getIdentifier( null, entitySupplier );
		BitSet dirtyPaths =
				typeContext().reindexingResolver().dirtySelfOrContainingFilter().filter( dirtyAssociationPathOrdinal );
		if ( dirtyPaths != null ) {
			getState( identifier ).addOrUpdate( entitySupplier, dirtyPaths, false, false );
		}
	}

	void planLoading() {
		for ( S state : statesPerId.values() ) {
			state.planLoading();
		}
	}

	void resolveDirty(boolean deleteOnly) {
		for ( S state : statesPerId.values() ) {
			state.resolveDirty( deleteOnly );
		}
	}

	void discard() {
		delegate.discard();
	}

	void clearStates() {
		this.mayRequireLoading = false;
		this.statesPerId.clear();
	}

	void process(PojoLoadingPlanProvider loadingPlanProvider) {
		if ( delegate == null ) {
			// Can happen with contained types depending on the strategy.
			return;
		}
		if ( sessionContext.configuredIndexingPlanFilter().isIncluded( typeContext().typeIdentifier() ) ) {
			for ( S state : statesPerId.values() ) {
				state.sendCommandsToDelegate( loadingPlanProvider );
			}
		}
	}

	CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter) {
		return delegate.executeAndReport( operationSubmitter );
	}

	abstract PojoWorkTypeContext<I, E> typeContext();

	abstract DocumentRouter<? super E> router();

	I toIdentifier(Object providedId, Supplier<E> entitySupplier) {
		return typeContext().identifierMapping().getIdentifier( providedId, entitySupplier );
	}

	final S getState(I identifier) {
		S state = statesPerId.get( identifier );
		if ( state == null ) {
			state = createState( identifier );
			statesPerId.put( identifier, state );
		}
		return state;
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return sessionContext.runtimeIntrospector();
	}

	@Override
	public PojoRawTypeIdentifier<?> detectContainingEntityType(Object containingEntity) {
		PojoRawTypeIdentifier<?> typeIdentifier = runtimeIntrospector().detectEntityType( containingEntity );
		if ( typeIdentifier == null ) {
			throw new AssertionFailure(
					"Attempted to detect entity type of object " + containingEntity
							+ " because a contained entity was modified,"
							+ " but this object does not seem to be an entity."
			);
		}
		return typeIdentifier;
	}

	// This is used for reindexing resolution only:
	// for indexing, we always propagate exceptions.
	@Override
	public void propagateOrIgnoreContainerExtractionException(RuntimeException exception) {
		if ( isIgnorableDataAccessThrowable( exception ) ) {
			return;
		}
		throw exception;
	}

	// This is used for reindexing resolution only:
	// for indexing, we always propagate exceptions.
	@Override
	public void propagateOrIgnorePropertyAccessException(RuntimeException exception) {
		if ( isIgnorableDataAccessThrowable( exception ) ) {
			return;
		}
		throw exception;
	}

	private boolean isIgnorableDataAccessThrowable(RuntimeException exception) {
		Throwable firstNonSearchThrowable = exception;
		while ( firstNonSearchThrowable instanceof SearchException ) {
			firstNonSearchThrowable = exception.getCause();
		}
		return firstNonSearchThrowable != null
				&& sessionContext.runtimeIntrospector().isIgnorableDataAccessThrowable( firstNonSearchThrowable );
	}

	protected abstract S createState(I identifier);

	boolean isDeleted(Object unproxiedObject) {
		E entity = typeContext().toEntity( unproxiedObject );
		I identifier = typeContext().identifierMapping().getIdentifierOrNull( entity );
		S state = statesPerId.get( identifier );
		if ( state == null ) {
			// No event whatsoever for that type, so definitely no delete event.
			return false;
		}
		return state.currentStatus == EntityStatus.ABSENT;
	}

	abstract class AbstractEntityState
			implements PojoImplicitReindexingResolverRootContext {
		final I identifier;
		private Supplier<E> entitySupplier;
		private Integer loadingOrdinal;

		EntityStatus initialStatus = EntityStatus.UNKNOWN;
		EntityStatus currentStatus = EntityStatus.UNKNOWN;

		private boolean updatedBecauseOfContained;
		private boolean forceSelfDirty;
		private boolean forceContainingDirty;
		private BitSet dirtyPaths;

		AbstractEntityState(I identifier) {
			this.identifier = identifier;
		}

		@Override
		public PojoImplicitReindexingResolverSessionContext sessionContext() {
			return sessionContext;
		}

		public boolean isDirtyForAddOrUpdate() {
			return delegate.isDirtyForAddOrUpdate( forceSelfDirty, forceContainingDirty, dirtyPaths );
		}

		@Override
		public boolean isDirtyForReindexingResolution(PojoPathFilter filter) {
			return forceContainingDirty || dirtyPaths != null && filter.test( dirtyPaths );
		}

		@Override
		public PojoRawTypeIdentifier<?> detectContainingEntityType(Object containingEntity) {
			return AbstractPojoTypeIndexingPlan.this.detectContainingEntityType( containingEntity );
		}

		// This is used for reindexing resolution only:
		// for indexing, we always propagate exceptions.
		@Override
		public void propagateOrIgnoreContainerExtractionException(RuntimeException exception) {
			AbstractPojoTypeIndexingPlan.this.propagateOrIgnoreContainerExtractionException( exception );
		}

		// This is used for reindexing resolution only:
		// for indexing, we always propagate exceptions.
		@Override
		public void propagateOrIgnorePropertyAccessException(RuntimeException exception) {
			AbstractPojoTypeIndexingPlan.this.propagateOrIgnorePropertyAccessException( exception );
		}

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.ABSENT;
			}
			currentStatus = EntityStatus.PRESENT;
			forceSelfDirty = true;
			forceContainingDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdate(Supplier<E> entitySupplier, BitSet dirtyPaths,
				boolean forceSelfDirty, boolean forceContainingDirty) {
			doAddOrUpdate( entitySupplier );
			doUpdateDirty( dirtyPaths, forceSelfDirty, forceContainingDirty );
		}

		// Should only be called on indexed types,
		// but it's simpler to implement this method for both indexed and contained types.
		void updateBecauseOfContained(Supplier<E> entitySupplier) {
			if ( currentStatus == EntityStatus.ABSENT ) {
				// This entity was deleted, but a containing entity still has a reference to it.
				// Someone probably just forgot to clear an association.
				// Just ignore the call.
				return;
			}
			doAddOrUpdate( entitySupplier );
			updatedBecauseOfContained = true;
			// We don't want contained entities that haven't been modified to trigger an update of their
			// containing entities.
			// Thus we don't set 'shouldResolveToReindex' to true here, but leave it as is.
		}

		void doAddOrUpdate(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			currentStatus = EntityStatus.PRESENT;
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.PRESENT;
			}
			currentStatus = EntityStatus.ABSENT;

			// Reindexing does not make sense for a deleted entity,
			// but we can still resolve containing entities to reindex.
			updatedBecauseOfContained = false;
			forceSelfDirty = false;
			forceContainingDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdateOrDelete(BitSet dirtyPaths, boolean forceSelfDirty, boolean forceContainingDirty) {
			this.entitySupplier = null;
			currentStatus = EntityStatus.UNKNOWN;
			doUpdateDirty( dirtyPaths, forceSelfDirty, forceContainingDirty );
		}

		protected void doUpdateDirty(BitSet dirtyPaths, boolean forceSelfDirty, boolean forceContainingDirty) {
			this.forceSelfDirty = this.forceSelfDirty || forceSelfDirty;
			this.forceContainingDirty = this.forceContainingDirty || forceContainingDirty;
			addDirtyPaths( dirtyPaths );
		}

		abstract void providedRoutes(DocumentRoutesDescriptor routes);

		abstract DocumentRoutesDescriptor providedRoutes();

		void planLoading() {
			if ( EntityStatus.ABSENT != currentStatus && entitySupplier == null ) {
				loadingOrdinal = root.loadingPlan().planLoading( typeContext(), identifier );
			}
		}

		void resolveDirty(boolean deleteOnly) {
			// In some configurations, we will perform reindexing resolution later,
			// after we reloaded the entities from the database;
			// but that's not possible for deleted entities,
			// so even those configurations perform reindexing resolution for deleted entities
			// in-session.
			if ( deleteOnly && !( initialStatus == EntityStatus.PRESENT && currentStatus == EntityStatus.ABSENT ) ) {
				return;
			}
			Supplier<E> entitySupplier = entitySupplierOrLoad( root );
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted before the current transaction started and there's nothing to resolve.
				return;
			}
			try {
				typeContext().reindexingResolver().resolveEntitiesToReindex( root, entitySupplier.get(), this );
			}
			catch (RuntimeException e) {
				EntityReference entityReference = sessionContext.mappingContext().entityReferenceFactoryDelegate()
						.create( typeContext().typeIdentifier(), typeContext().entityName(), identifier );
				throw log.errorResolvingEntitiesToReindex( entityReference, e.getMessage(), e );
			}
			typeContext().resolveEntitiesToReindex( root, sessionContext, identifier,
					entitySupplier, this );
		}

		void sendCommandsToDelegate(PojoLoadingPlanProvider loadingPlanProvider) {
			if ( EntityStatus.UNKNOWN.equals( currentStatus ) ) {
				Supplier<E> entitySupplier = entitySupplierOrLoad( loadingPlanProvider );
				currentStatus = entitySupplier != null ? EntityStatus.PRESENT : EntityStatus.ABSENT;
			}
			switch ( currentStatus ) {
				case PRESENT:
					switch ( initialStatus ) {
						case ABSENT:
							delegateAdd( loadingPlanProvider );
							return;
						case PRESENT:
						case UNKNOWN:
							delegateAddOrUpdate( loadingPlanProvider );
							return;
					}
					break;
				case ABSENT:
					switch ( initialStatus ) {
						case ABSENT:
							// The entity was added, then deleted in the same plan.
							// Don't do anything.
							return;
						case UNKNOWN:
						case PRESENT:
							delegateDelete();
							return;
					}
					break;
			}
		}

		void delegateAdd(PojoLoadingPlanProvider loadingPlanProvider) {
			Supplier<E> entitySupplier = entitySupplierOrLoad( loadingPlanProvider );
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted and there's nothing to add.
				// A delete event should follow at some point.
				return;
			}

			DocumentRouteDescriptor currentRoute = router()
					.currentRoute( identifier, entitySupplier, providedRoutes(), sessionContext );
			// We don't care about previous routes: the add() operation expects that the document isn't in the index yet.
			if ( currentRoute == null ) {
				// The routing bridge decided the entity should not be indexed.
				// There's nothing to do.
				return;
			}
			delegate.add( identifier, currentRoute, entitySupplier );
		}

		void delegateAddOrUpdate(PojoLoadingPlanProvider loadingPlanProvider) {
			boolean updateBecauseOfDirty = isDirtyForAddOrUpdate();
			if ( !updatedBecauseOfContained && !updateBecauseOfDirty ) {
				// Optimization: the update is not relevant to indexing
				return;
			}

			Supplier<E> entitySupplier = entitySupplierOrLoad( loadingPlanProvider );
			if ( entitySupplier == null ) {
				// We couldn't retrieve the entity.
				// Assume it was deleted and there's nothing to add or update.
				// A delete event should follow at some point.
				return;
			}

			DocumentRoutesDescriptor routes = router()
					.routes( identifier, entitySupplier, providedRoutes(), sessionContext );
			if ( routes.currentRoute() == null && routes.previousRoutes().isEmpty() ) {
				// The routing bridge decided the entity should not be indexed, and that it wasn't indexed previously.
				// There's nothing to do.
				return;
			}
			delegate.addOrUpdate( identifier, routes, entitySupplier,
					forceSelfDirty, forceContainingDirty, dirtyPaths,
					updatedBecauseOfContained, updateBecauseOfDirty );
		}

		void delegateDelete() {
			Supplier<E> entitySupplier = entitySupplierNoLoad();

			DocumentRouter<? super E> router;
			if ( entitySupplier != null ) {
				router = router();
			}
			else {
				// Purge: entity is not available and we can't route according to its state.
				// We can use the provided routing keys, though, which is what the no-op router does.
				router = NoOpDocumentRouter.INSTANCE;
			}

			DocumentRoutesDescriptor routes = router
					.routes( identifier, entitySupplier, providedRoutes(), sessionContext );
			if ( routes.currentRoute() == null && routes.previousRoutes().isEmpty() ) {
				// The routing bridge decided the entity should not be indexed, and that it wasn't indexed previously.
				// There's nothing to do.
				return;
			}
			delegate.delete( identifier, routes, entitySupplier );
		}

		Supplier<E> entitySupplierNoLoad() {
			return entitySupplier;
		}

		Supplier<E> entitySupplierOrLoad(PojoLoadingPlanProvider loadingPlanProvider) {
			if ( entitySupplier == null && loadingOrdinal != null ) {
				E loaded = loadingPlanProvider.loadingPlan().retrieve( typeContext(), loadingOrdinal );
				entitySupplier = typeContext().toEntitySupplier( sessionContext, loaded );
				loadingOrdinal = null;
			}
			return entitySupplier;
		}

		private void addDirtyPaths(BitSet newDirtyPaths) {
			if ( newDirtyPaths == null ) {
				return;
			}
			if ( dirtyPaths == null ) {
				dirtyPaths = new BitSet();
			}
			dirtyPaths.or( newDirtyPaths );
		}
	}

	protected enum EntityStatus {
		UNKNOWN,
		PRESENT,
		ABSENT
	}
}
