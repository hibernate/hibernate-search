/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;

class ChangesetPojoIndexedTypeWorker<I, E, D extends DocumentElement> extends PojoTypeWorker {

	private final PojoIndexedTypeManager<I, E, D> typeManager;
	private final ChangesetIndexWorker<D> delegate;

	// Use a LinkedHashMap for stable ordering across JVMs
	private final Map<I, WorkPlanPerDocument> workPlansPerId = new LinkedHashMap<>();

	ChangesetPojoIndexedTypeWorker(PojoIndexedTypeManager<I, E, D> typeManager, PojoSessionContext sessionContext,
			ChangesetIndexWorker<D> delegate) {
		super( sessionContext );
		this.typeManager = typeManager;
		this.delegate = delegate;
	}

	@Override
	void add(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		getWork( identifier ).add( entitySupplier );
	}

	@Override
	void update(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		getWork( identifier ).update( entitySupplier );
	}

	@Override
	void delete(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		getWork( identifier ).delete( entitySupplier );
	}

	public void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( null, entitySupplier );
		if ( !workPlansPerId.containsKey( identifier ) ) {
			getWork( identifier ).updateBecauseOfContained( entitySupplier );
		}
		// If the entry is already there, no need for an additional update
	}

	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		for ( WorkPlanPerDocument workPerDocument : workPlansPerId.values() ) {
			workPerDocument.resolveDirty( containingEntityCollector );
		}
	}

	void prepare() {
		sendWorksToDelegate();
		getDelegate().prepare();
	}

	CompletableFuture<?> execute() {
		sendWorksToDelegate();
		/*
		 * No need to call prepare() here:
		 * delegates are supposed to handle execute() even without a prior call to prepare().
		 */
		return delegate.execute();
	}

	private WorkPlanPerDocument getWork(I identifier) {
		WorkPlanPerDocument work = workPlansPerId.get( identifier );
		if ( work == null ) {
			work = new WorkPlanPerDocument( identifier );
			workPlansPerId.put( identifier, work );
		}
		return work;
	}

	private ChangesetIndexWorker<D> getDelegate() {
		return delegate;
	}

	private void sendWorksToDelegate() {
		try {
			workPlansPerId.values().forEach( WorkPlanPerDocument::sendWorkToDelegate );
		}
		finally {
			workPlansPerId.clear();
		}
	}

	private class WorkPlanPerDocument {
		private final I identifier;
		private Supplier<E> entitySupplier;

		private boolean delete;
		private boolean add;

		private boolean shouldResolveDirty;

		private WorkPlanPerDocument(I identifier) {
			this.identifier = identifier;
		}

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveDirty = true;
			add = true;
		}

		void update(Supplier<E> entitySupplier) {
			doUpdate( entitySupplier, true );
		}

		void updateBecauseOfContained(Supplier<E> entitySupplier) {
			/*
			 * Make sure that containing entities that haven't been modified will not trigger an update of their
			 * containing entities, unless those containing entities embed other entities in their index,
			 * and those entities have been modified.
			 */
			doUpdate( entitySupplier, false );
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( add && !delete ) {
				/*
				 * We called add() in the same changeset, so we don't expect the document to be in the index.
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

		void doUpdate(Supplier<E> entitySupplier, boolean shouldResolveDirty) {
			this.entitySupplier = entitySupplier;
			/*
			 * If add is true, either this is already an update (in which case we don't need to change the flags)
			 * or we called add() in the same changeset (in which case we don't expect the document to be in the index).
			 */
			if ( !add ) {
				delete = true;
				add = true;
			}
			if ( shouldResolveDirty ) {
				this.shouldResolveDirty = true;
			}
		}

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveDirty ) {
				shouldResolveDirty = false; // Avoid infinite looping
				typeManager.resolveEntitiesToReindex( containingEntityCollector, entitySupplier );
			}
		}

		void sendWorkToDelegate() {
			DocumentReferenceProvider referenceProvider =
					typeManager.toDocumentReferenceProvider( sessionContext, identifier, entitySupplier );
			if ( add ) {
				if ( delete ) {
					delegate.update( referenceProvider, typeManager.toDocumentContributor( entitySupplier ) );
				}
				else {
					delegate.add( referenceProvider, typeManager.toDocumentContributor( entitySupplier ) );
				}
			}
			else if ( delete ) {
				delegate.delete( referenceProvider );
			}
		}
	}

}
