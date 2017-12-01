/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;

/**
 * A wrapper around {@link ChangesetIndexWorker} adding an optimization:
 * it will not trigger any processing on documents that are added/updated,
 * then deleted in the same changeset.
 *
 * @author Yoann Rodiere
 */
public class SimplifyingChangesetIndexWorker<D> implements ChangesetIndexWorker<D> {

	private final ChangesetIndexWorker<D> delegate;

	/*
	 * We use a LinkedHashMap to ensure the order will be stable from one run to another.
	 * This changes everything when debugging...
	 */
	private final Map<String, WorkPerDocument> worksPerDocument = new LinkedHashMap<>();

	public SimplifyingChangesetIndexWorker(ChangesetIndexWorker<D> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void add(DocumentReferenceProvider documentReferenceProvider, DocumentContributor<D> documentContributor) {
		getWork( documentReferenceProvider ).add( documentContributor );

	}

	@Override
	public void update(DocumentReferenceProvider documentReferenceProvider, DocumentContributor<D> documentContributor) {
		getWork( documentReferenceProvider ).update( documentContributor );
	}

	@Override
	public void delete(DocumentReferenceProvider documentReferenceProvider) {
		getWork( documentReferenceProvider ).delete();
	}

	@Override
	public void prepare() {
		doPrepare();
		delegate.prepare();
	}

	@Override
	public CompletableFuture<?> execute() {
		/*
		 * No need to call prepare() on the delegate here:
		 * the delegate is supposed to handle execute() even without a prior call to prepare().
		 */
		doPrepare();
		return delegate.execute();
	}

	void doPrepare() {
		try {
			worksPerDocument.values().forEach( WorkPerDocument::doDelegate );
		}
		finally {
			worksPerDocument.clear();
		}
	}

	private WorkPerDocument getWork(DocumentReferenceProvider documentReferenceProvider) {
		String identifier = documentReferenceProvider.getIdentifier();
		WorkPerDocument work = worksPerDocument.get( identifier );
		if ( work == null ) {
			work = new WorkPerDocument( documentReferenceProvider );
			worksPerDocument.put( identifier, work );
		}
		return work;
	}

	private class WorkPerDocument {

		private final DocumentReferenceProvider documentReferenceProvider;
		private DocumentContributor<D> documentContributor;

		private boolean delete;
		private boolean add;

		private WorkPerDocument(DocumentReferenceProvider documentReferenceProvider) {
			this.documentReferenceProvider = documentReferenceProvider;
		}

		public void add(DocumentContributor<D> contributor) {
			add = true;
			documentContributor = contributor;
		}

		public void update(DocumentContributor<D> contributor) {
			/*
			 * If add is true, either this is already an update (in which case we don't need to change the flags)
			 * or we called add() in the same changeset (in which case we don't expect the document to be in the index).
			 */
			if ( !add ) {
				delete = true;
				add = true;
			}
			documentContributor = contributor;
		}

		public void delete() {
			if ( add && !delete ) {
				/*
				 * We called add() in the same changeset, so we don't expect the document to be in the index.
				 * Don't delete, just cancel the addition.
				 */
				add = false;
				delete = false;
			}
			else {
				add = false;
				delete = true;
			}
			documentContributor = null;
		}

		public void doDelegate() {
			if ( add ) {
				if ( delete ) {
					delegate.update( documentReferenceProvider, documentContributor );
				}
				else {
					delegate.add( documentReferenceProvider, documentContributor );
				}
			}
			else if ( delete ) {
				delegate.delete( documentReferenceProvider );
			}
		}

	}

}
