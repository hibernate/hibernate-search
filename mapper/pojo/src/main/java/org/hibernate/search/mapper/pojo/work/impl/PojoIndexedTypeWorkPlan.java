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
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public class PojoIndexedTypeWorkPlan<I, E, D extends DocumentElement> extends AbstractPojoTypeWorkPlan {

	private final PojoIndexedTypeManager<I, E, D> typeManager;
	private final IndexWorkPlan<D> delegate;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<I, IndexedEntityWorkPlan> workPlansPerId = new LinkedHashMap<>();

	public PojoIndexedTypeWorkPlan(PojoIndexedTypeManager<I, E, D> typeManager, AbstractPojoSessionContextImplementor sessionContext,
			IndexWorkPlan<D> delegate) {
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
	void update(Object providedId, Object entity, String... dirtyPaths) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		getWork( identifier ).update( entitySupplier, dirtyPaths );
	}

	@Override
	void delete(Object providedId, Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( providedId, entitySupplier );
		getWork( identifier ).delete( entitySupplier );
	}

	@Override
	void purge(Object providedId) {
		I identifier = typeManager.getIdentifierMapping().getIdentifier( providedId );
		getWork( identifier ).purge();
	}

	void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeManager.toEntitySupplier( sessionContext, entity );
		I identifier = typeManager.getIdentifierMapping().getIdentifier( null, entitySupplier );
		if ( !workPlansPerId.containsKey( identifier ) ) {
			getWork( identifier ).updateBecauseOfContained( entitySupplier );
		}
		// If the entry is already there, no need for an additional update
	}

	void resolveDirty(PojoReindexingCollector containingEntityCollector) {
		for ( IndexedEntityWorkPlan workPerDocument : workPlansPerId.values() ) {
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

	private IndexedEntityWorkPlan getWork(I identifier) {
		IndexedEntityWorkPlan work = workPlansPerId.get( identifier );
		if ( work == null ) {
			work = new IndexedEntityWorkPlan( identifier );
			workPlansPerId.put( identifier, work );
		}
		return work;
	}

	private IndexWorkPlan<D> getDelegate() {
		return delegate;
	}

	private void sendWorksToDelegate() {
		try {
			workPlansPerId.values().forEach( IndexedEntityWorkPlan::sendWorkToDelegate );
		}
		finally {
			workPlansPerId.clear();
		}
	}

	private class IndexedEntityWorkPlan {
		private final I identifier;
		private Supplier<E> entitySupplier;

		private boolean delete;
		private boolean add;

		private boolean shouldResolveToReindex;
		private boolean considerAllDirty;
		private boolean updatedBecauseOfContained;
		private Set<String> dirtyPaths;

		private IndexedEntityWorkPlan(I identifier) {
			this.identifier = identifier;
		}

		void add(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			shouldResolveToReindex = true;
			add = true;
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
				for ( String dirtyPropertyName : dirtyPaths ) {
					addDirtyPath( dirtyPropertyName );
				}
			}
		}

		void updateBecauseOfContained(Supplier<E> entitySupplier) {
			doUpdate( entitySupplier );
			updatedBecauseOfContained = true;
			/*
			 * We don't want contained entities that haven't been modified to trigger an update of their
			 * containing entities.
			 * Thus we don't set 'shouldResolveToReindex' to true here, but leave it as is.
			 */
		}

		void delete(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			if ( add && !delete ) {
				/*
				 * We called add() in the same plan, so we don't expect the document to be in the index.
				 * Don't delete, just cancel the addition.
				 */
				shouldResolveToReindex = false;
				considerAllDirty = false;
				updatedBecauseOfContained = false;
				dirtyPaths = null;
				add = false;
				delete = false;
			}
			else {
				add = false;
				delete = true;
			}
		}

		void purge() {
			// This is a purge: do not resolve reindexing
			entitySupplier = null;
			shouldResolveToReindex = false;
			// This is a purge: force deletion even if it doesn't seem this document was added
			considerAllDirty = false;
			dirtyPaths = null;
			add = false;
			delete = true;
		}

		void resolveDirty(PojoReindexingCollector containingEntityCollector) {
			if ( shouldResolveToReindex ) {
				shouldResolveToReindex = false; // Avoid infinite looping
				typeManager.resolveEntitiesToReindex(
						containingEntityCollector, sessionContext.getRuntimeIntrospector(), entitySupplier,
						considerAllDirty ? null : dirtyPaths
				);
			}
		}

		void sendWorkToDelegate() {
			DocumentReferenceProvider referenceProvider =
					typeManager.toDocumentReferenceProvider( sessionContext, identifier, entitySupplier );
			if ( add ) {
				if ( delete ) {
					if ( considerAllDirty || updatedBecauseOfContained || typeManager.requiresSelfReindexing( dirtyPaths ) ) {
						delegate.update(
								referenceProvider,
								typeManager.toDocumentContributor( entitySupplier, sessionContext )
						);
					}
				}
				else {
					delegate.add(
							referenceProvider,
							typeManager.toDocumentContributor( entitySupplier, sessionContext )
					);
				}
			}
			else if ( delete ) {
				delegate.delete( referenceProvider );
			}
		}

		private void doUpdate(Supplier<E> entitySupplier) {
			this.entitySupplier = entitySupplier;
			/*
			 * If add is true, either this is already an update (in which case we don't need to change the flags)
			 * or we called add() in the same plan (in which case we don't expect the document to be in the index).
			 */
			if ( !add ) {
				delete = true;
				add = true;
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
