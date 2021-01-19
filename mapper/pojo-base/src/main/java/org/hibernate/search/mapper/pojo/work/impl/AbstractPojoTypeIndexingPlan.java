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

	abstract class AbstractEntityState
			implements PojoImplicitReindexingResolverRootContext {
		final I identifier;
		Supplier<E> entitySupplier;

		EntityStatus initialStatus = EntityStatus.UNKNOWN;
		EntityStatus currentStatus = EntityStatus.UNKNOWN;

		boolean shouldResolveToReindex;
		boolean considerAllDirty;
		BitSet dirtyPaths;

		AbstractEntityState(I identifier) {
			this.identifier = identifier;
		}

		@Override
		public PojoImplicitReindexingResolverSessionContext sessionContext() {
			return sessionContext;
		}

		@Override
		public BitSet dirtinessState() {
			return dirtyPaths;
		}

		void add(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.ABSENT;
			}
			currentStatus = EntityStatus.PRESENT;
		}

		void update(Supplier<E> entitySupplier, String providedRoutingKey) {
			doUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void update(Supplier<E> entitySupplier, String providedRoutingKey, String... dirtyPathsAsStrings) {
			doUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			if ( !considerAllDirty ) {
				addDirtyPaths( dirtyPathsAsStrings );
			}
		}

		void doUpdate(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.PRESENT;
			}
			currentStatus = EntityStatus.PRESENT;
		}

		void delete(Supplier<E> entitySupplier, String providedRoutingKey) {
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

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				typeContext().resolveEntitiesToReindex( containingEntityCollector, sessionContext, identifier,
						entitySupplier, this );
			}
		}

		private void addDirtyPaths(String[] dirtyPathsAsStrings) {
			if ( dirtyPaths == null ) {
				dirtyPaths = new BitSet();
			}
			typeContext().dirtySelfOrContainingFilter().setAccepted( dirtyPaths, dirtyPathsAsStrings );
		}
	}

	protected enum EntityStatus {
		UNKNOWN,
		PRESENT,
		ABSENT;
	}
}
