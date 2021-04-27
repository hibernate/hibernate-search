/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.spi.PojoImplicitReindexingResolverSessionContext;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 * @param <S> The type of per-instance state.
 */
abstract class AbstractPojoTypeIndexingPlan<I, E, S extends AbstractPojoTypeIndexingPlan<I, E, S>.AbstractEntityState> {

	final PojoWorkSessionContext sessionContext;
	final PojoIndexingPlanImpl root;

	// Use a LinkedHashMap for deterministic iteration
	final Map<I, S> statesPerId = new LinkedHashMap<>();

	AbstractPojoTypeIndexingPlan(PojoWorkSessionContext sessionContext, PojoIndexingPlanImpl root) {
		this.sessionContext = sessionContext;
		this.root = root;
	}

	void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.add( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.addOrUpdate( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity, BitSet dirtyPaths) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.addOrUpdate( entitySupplier, dirtyPaths );
		state.providedRoutes( providedRoutes );
	}

	void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		S state = getState( identifier );
		state.delete( entitySupplier );
		state.providedRoutes( providedRoutes );
	}

	void planLoading() {
		for ( S state : statesPerId.values() ) {
			state.planLoading();
		}
	}

	void resolveDirty() {
		for ( S state : statesPerId.values() ) {
			state.resolveDirty();
		}
	}

	abstract PojoWorkTypeContext<E> typeContext();

	abstract I toIdentifier(Object providedId, Supplier<E> entitySupplier);

	final S getState(I identifier) {
		S state = statesPerId.get( identifier );
		if ( state == null ) {
			state = createState( identifier );
			statesPerId.put( identifier, state );
		}
		return state;
	}

	protected abstract S createState(I identifier);

	abstract class AbstractEntityState
			implements PojoImplicitReindexingResolverRootContext {
		final I identifier;
		private Supplier<E> entitySupplier;
		private Integer loadingOrdinal;

		EntityStatus initialStatus = EntityStatus.UNKNOWN;
		EntityStatus currentStatus = EntityStatus.UNKNOWN;

		private boolean shouldResolveToReindex;
		private boolean considerAllDirty;
		private BitSet dirtyPaths;

		AbstractEntityState(I identifier) {
			this.identifier = identifier;
		}

		@Override
		public PojoImplicitReindexingResolverSessionContext sessionContext() {
			return sessionContext;
		}

		@Override
		public boolean isDirty(PojoPathFilter filter) {
			return considerAllDirty || dirtyPaths != null && filter.test( dirtyPaths );
		}

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.ABSENT;
			}
			currentStatus = EntityStatus.PRESENT;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdate(Supplier<E> entitySupplier) {
			doAddOrUpdate( entitySupplier );
			shouldResolveToReindex = true;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdate(Supplier<E> entitySupplier, BitSet dirtyPaths) {
			doAddOrUpdate( entitySupplier );
			shouldResolveToReindex = true;
			if ( !considerAllDirty ) {
				addDirtyPaths( dirtyPaths );
			}
		}

		void doAddOrUpdate(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.PRESENT;
			}
			currentStatus = EntityStatus.PRESENT;
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.PRESENT;
			}
			currentStatus = EntityStatus.ABSENT;

			// Reindexing does not make sense for a deleted entity
			shouldResolveToReindex = false;
			considerAllDirty = false;
			dirtyPaths = null;
		}

		abstract void providedRoutes(DocumentRoutesDescriptor routes);

		void planLoading() {
			if ( EntityStatus.PRESENT == currentStatus && entitySupplier == null ) {
				loadingOrdinal = root.loadingPlan().planLoading( typeContext(), identifier );
			}
		}

		void resolveDirty() {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				Supplier<E> entitySupplier = entitySupplierOrLoad();
				if ( entitySupplier == null ) {
					// We couldn't retrieve the entity.
					// Assume it was deleted and there's nothing to resolve.
					return;
				}
				typeContext().resolveEntitiesToReindex( root, sessionContext, identifier,
						entitySupplier, this );
			}
		}

		Supplier<E> entitySupplierOrLoad() {
			if ( entitySupplier == null && loadingOrdinal != null ) {
				E loaded = root.loadingPlan().retrieve( typeContext(), loadingOrdinal );
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
