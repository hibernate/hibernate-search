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

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.spi.PojoImplicitReindexingResolverSessionContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.SearchException;

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

	void addOrUpdate(Object providedId, String providedRoutingKey, Object entity) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		getState( identifier ).addOrUpdate( entitySupplier, providedRoutingKey );
	}

	void addOrUpdate(Object providedId, String providedRoutingKey, Object entity, String... dirtyPaths) {
		Supplier<E> entitySupplier = typeContext().toEntitySupplier( sessionContext, entity );
		I identifier = toIdentifier( providedId, entitySupplier );
		getState( identifier ).addOrUpdate( entitySupplier, providedRoutingKey, dirtyPaths );
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
			implements PojoImplicitReindexingResolverRootContext<Set<String>> {
		final I identifier;
		Supplier<E> entitySupplier;

		EntityStatus initialStatus = EntityStatus.UNKNOWN;
		EntityStatus currentStatus = EntityStatus.UNKNOWN;

		boolean shouldResolveToReindex;
		boolean considerAllDirty;
		Set<String> dirtyPaths;

		AbstractEntityState(I identifier) {
			this.identifier = identifier;
		}

		@Override
		public PojoImplicitReindexingResolverSessionContext sessionContext() {
			return sessionContext;
		}

		@Override
		public Set<String> dirtinessState() {
			return dirtyPaths;
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
			return firstNonSearchThrowable != null &&
					sessionContext.runtimeIntrospector().isIgnorableDataAccessThrowable( firstNonSearchThrowable );
		}

		void add(Supplier<E> entitySupplier, String providedRoutingKey) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			if ( EntityStatus.UNKNOWN.equals( initialStatus ) ) {
				initialStatus = EntityStatus.ABSENT;
			}
			currentStatus = EntityStatus.PRESENT;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdate(Supplier<E> entitySupplier, String providedRoutingKey) {
			doAddOrUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void addOrUpdate(Supplier<E> entitySupplier, String providedRoutingKey, String... dirtyPaths) {
			doAddOrUpdate( entitySupplier, providedRoutingKey );
			shouldResolveToReindex = true;
			if ( !considerAllDirty ) {
				for ( String dirtyPath : dirtyPaths ) {
					addDirtyPath( dirtyPath );
				}
			}
		}

		void doAddOrUpdate(Supplier<E> entitySupplier, String providedRoutingKey) {
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

		private void addDirtyPath(String dirtyPath) {
			if ( dirtyPaths == null ) {
				dirtyPaths = new HashSet<>();
			}
			dirtyPaths.add( dirtyPath );
		}
	}

	protected enum EntityStatus {
		UNKNOWN,
		PRESENT,
		ABSENT;
	}
}
