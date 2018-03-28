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

		private boolean delete;
		private boolean add;

		private boolean shouldResolveDirty;

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveDirty = true;
			add = true;
		}

		void update(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			/*
			 * If add is true, either this is already an update (in which case we don't need to change the flags)
			 * or we called add() in the same changeset (in which case we don't expect the document to be in the index).
			 */
			if ( !add ) {
				delete = true;
				add = true;
			}
			this.shouldResolveDirty = true;
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( add && !delete ) {
				/*
				 * We called add() in the same changeset, so we don't expect the entity to be contained
				 * in existing documents.
				 * Don't delete, just cancel the addition.
				 */
				shouldResolveDirty = false;
				add = false;
				delete = false;
			}
			else {
				add = false;
				delete = true;
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
