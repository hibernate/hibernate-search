/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeWorkPlan<E> extends AbstractPojoTypeWorkPlan {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkContainedTypeContext<E> typeContext;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<Object, ContainedEntityWorkPlan> workPlansPerId = new LinkedHashMap<>();

	public PojoContainedTypeWorkPlan(PojoWorkContainedTypeContext<E> typeContext,
			AbstractPojoBackendSessionContext sessionContext) {
		super( sessionContext );
		this.typeContext = typeContext;
	}

	@Override
	void add(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).add( entitySupplier );
	}

	@Override
	void update(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).update( entitySupplier );
	}

	@Override
	void update(Object providedId, Object entity, String... dirtyPaths) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).update( entitySupplier, dirtyPaths );
	}

	@Override
	void delete(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).delete( entitySupplier );
	}

	@Override
	void purge(Object providedId) {
		throw log.cannotPurgeNonIndexedContainedType( typeContext.getJavaClass(), providedId );
	}

	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		for ( ContainedEntityWorkPlan workPerDocument : workPlansPerId.values() ) {
			workPerDocument.resolveDirty( containingEntityCollector );
		}
	}

	private ContainedEntityWorkPlan getWork(Object identifier) {
		ContainedEntityWorkPlan work = workPlansPerId.get( identifier );
		if ( work == null ) {
			work = new ContainedEntityWorkPlan();
			workPlansPerId.put( identifier, work );
		}
		return work;
	}

	private class ContainedEntityWorkPlan {
		private Supplier<E> entitySupplier;

		private Boolean createdInThisPlan;

		private boolean shouldResolveToReindex;
		private boolean considerAllDirty;
		private Set<String> dirtyPaths;

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			if ( createdInThisPlan == null ) {
				// No update yet, so we actually did create the entity in this plan
				createdInThisPlan = true;
			}
		}

		void update(Supplier<E> entitySupplier) {
			doUpdate( entitySupplier );
			shouldResolveToReindex = true;
			considerAllDirty = true;
			dirtyPaths = null;
		}

		void update(Supplier<E> entitySupplier, String... dirtyPaths) {
			doUpdate( entitySupplier );
			shouldResolveToReindex = true;
			if ( !considerAllDirty ) {
				for ( String dirtyPath : dirtyPaths ) {
					addDirtyPath( dirtyPath );
				}
			}
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( createdInThisPlan == null ) {
				// No add or update yet, and we're performing a delete, so we did not create the entity in this plan
				createdInThisPlan = false;
			}
			else if ( createdInThisPlan ) {
				/*
				 * We called the first add() in the same plan, so we don't expect the entity to be contained
				 * in existing documents.
				 * Cancel everything.
				 */
				shouldResolveToReindex = false;
				considerAllDirty = false;
				dirtyPaths = null;
				createdInThisPlan = null;
			}
		}

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				typeContext.resolveEntitiesToReindex(
						containingEntityCollector, sessionContext.getRuntimeIntrospector(), entitySupplier,
						considerAllDirty ? null : dirtyPaths
				);
			}
		}

		private void doUpdate(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( createdInThisPlan == null ) {
				// No add yet, and we're performing an update, so we did not create the entity in this plan
				createdInThisPlan = false;
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
