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

import org.hibernate.search.engine.backend.document.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;

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
	public void add(String id, DocumentContributor<D> documentContributor) {
		getWork( id ).add( documentContributor );

	}

	@Override
	public void update(String id, DocumentContributor<D> documentContributor) {
		getWork( id ).update( documentContributor );
	}

	@Override
	public void delete(String id) {
		getWork( id ).delete();
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			worksPerDocument.values().forEach( WorkPerDocument::doDelegate );
			return delegate.execute();
		}
		finally {
			worksPerDocument.clear();
		}
	}

	private WorkPerDocument getWork(String id) {
		return worksPerDocument.computeIfAbsent( id, WorkPerDocument::new );
	}

	private class WorkPerDocument {

		private final String id;
		private DocumentContributor<D> documentContributor;

		private boolean delete;
		private boolean add;

		private WorkPerDocument(String id) {
			this.id = id;
		}

		public void add(DocumentContributor<D> contributor) {
			delete = false; // An "add" work means we don't expect the document to be in the index
			add = true;
			documentContributor = contributor;
		}

		public void update(DocumentContributor<D> contributor) {
			delete = true;
			add = true;
			documentContributor = contributor;
		}

		public void delete() {
			if ( add && !delete ) {
				// Initial add in the same changeset: no need to delete, just do not add.
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
					delegate.update( id, documentContributor );
				}
				else {
					delegate.add( id, documentContributor );
				}
			}
			else if ( delete ) {
				delegate.delete( id );
			}
		}

	}

}
