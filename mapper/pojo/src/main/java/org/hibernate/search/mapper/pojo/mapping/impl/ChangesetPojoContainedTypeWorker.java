/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;

/**
 * @param <E> The contained entity type.
 */
class ChangesetPojoContainedTypeWorker<E> extends PojoTypeWorker {

	private final PojoContainedTypeManager<E> typeManager;

	// Use a LinkedHashMap for stable ordering across JVMs
	private final Map<Object, WorkPlanPerDocument> workPlansPerId = new LinkedHashMap<>();

	ChangesetPojoContainedTypeWorker(PojoContainedTypeManager<E> typeManager, PojoSessionContext sessionContext) {
		super( sessionContext );
		this.typeManager = typeManager;
	}

	@Override
	public void add(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).add( entitySupplier );
	}

	@Override
	public void update(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).update( entitySupplier );
	}

	@Override
	public void delete(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		getWork( providedId ).delete( entitySupplier );
	}

	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		for ( WorkPlanPerDocument workPerDocument : workPlansPerId.values() ) {
			workPerDocument.resolveDirty( containingEntityCollector );
		}
	}

	private WorkPlanPerDocument getWork(Object identifier) {
		WorkPlanPerDocument work = workPlansPerId.get( identifier );
		if ( work == null ) {
			work = new WorkPlanPerDocument();
			workPlansPerId.put( identifier, work );
		}
		return work;
	}

	private class WorkPlanPerDocument {
		private Supplier<E> entitySupplier;

		private Boolean createdInThisChangeset;

		private boolean shouldResolveDirty;

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveDirty = true;
			if ( createdInThisChangeset == null ) {
				// No update yet, so we actually did create the entity in this changeset
				createdInThisChangeset = true;
			}
		}

		void update(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			this.shouldResolveDirty = true;
			if ( createdInThisChangeset == null ) {
				// No add yet, and we're performing an update, so we did not create the entity in this changeset
				createdInThisChangeset = false;
			}
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( createdInThisChangeset == null ) {
				// No add or update yet, and we're performing a delete, so we did not create the entity in this changeset
				createdInThisChangeset = false;
			}
			else if ( createdInThisChangeset ) {
				/*
				 * We called the first add() in the same changeset, so we don't expect the entity to be contained
				 * in existing documents.
				 * Cancel everything.
				 */
				shouldResolveDirty = false;
				createdInThisChangeset = null;
			}
		}

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveDirty ) {
				shouldResolveDirty = false; // Avoid infinite looping
				typeManager.resolveEntitiesToReindex( containingEntityCollector,
						sessionContext.getRuntimeIntrospector(), entitySupplier );
			}
		}
	}

}
