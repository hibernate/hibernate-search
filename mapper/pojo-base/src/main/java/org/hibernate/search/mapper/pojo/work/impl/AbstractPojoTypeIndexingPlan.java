/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 * @param <S> The type of per-instance state.
 */
abstract class AbstractPojoTypeIndexingPlan<I, E, S extends AbstractPojoTypeIndexingPlan<I, E, S>.AbstractEntityState> {

	final PojoWorkSessionContext<?> sessionContext;

	// Use a LinkedHashMap for deterministic iteration
	final Map<I, S> statesPerId = new LinkedHashMap<>();

	AbstractPojoTypeIndexingPlan(PojoWorkSessionContext<?> sessionContext) {
		this.sessionContext = sessionContext;
	}

	void add(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		getState( identifier ).add( entitySupplier, providedRoutingKey );
	}

	void update(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		getState( identifier ).update( entitySupplier, providedRoutingKey );
	}

	void update(Object providedId, String providedRoutingKey, Object entity, String... dirtyPaths) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		getState( identifier ).update( entitySupplier, providedRoutingKey, dirtyPaths );
	}

	void delete(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		getState( identifier ).delete( entitySupplier, providedRoutingKey );
	}

	abstract void purge(Object providedId, String providedRoutingKey);

	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		for ( S state : statesPerId.values() ) {
			state.resolveDirty( containingEntityCollector );
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

	abstract class AbstractEntityState {
		final I identifier;
		Supplier<E> entitySupplier;

		boolean deleted;
		boolean added;

		boolean shouldResolveToReindex;
		boolean considerAllDirty;
		Set<String> dirtyPaths;

		AbstractEntityState(I identifier) {
			this.identifier = identifier;
		}

		void add(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			added = true;
		}

		void update(Supplier<E> entitySupplier, String providedRoutingKey) {
			doUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void update(Supplier<E> entitySupplier, String providedRoutingKey, String... dirtyPaths) {
			doUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			if ( !considerAllDirty ) {
				for ( String dirtyPath : dirtyPaths ) {
					addDirtyPath( dirtyPath );
				}
			}
		}

		void doUpdate(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			if ( !added ) {
				deleted = true;
				added = true;
			}
			// else: If add is true, either this is already an update (in which case update + update = update)
			// or we called add() in the same plan (in which case add + update = add).
			// In any case we don't need to change anything.
		}

		void delete(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			if ( added && !deleted ) {
				// We called add() in the same plan, so the entity didn't exist.
				// Don't delete, just cancel the addition.
				added = false;
				deleted = false;
			}
			else {
				// No add or update yet, or already deleted.
				// Either way, delete.
				added = false;
				deleted = true;
			}

			// Reindexing does not make sense for a deleted entity
			shouldResolveToReindex = false;
			considerAllDirty = false;
			dirtyPaths = null;
		}

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				typeContext().resolveEntitiesToReindex(
						containingEntityCollector, sessionContext, identifier, entitySupplier,
						considerAllDirty ? null : dirtyPaths
				);
			}
		}

		private void addDirtyPath(String dirtyPath) {
			if ( dirtyPaths == null ) {
				dirtyPaths = new HashSet<>();
			}
			dirtyPaths.add( dirtyPath );
		}
	}


}
