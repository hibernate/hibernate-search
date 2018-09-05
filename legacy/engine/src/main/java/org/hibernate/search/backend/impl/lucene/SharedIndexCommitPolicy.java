/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Commit policy for a shared index writer, that must be flushed (and closed) after each changeset
 *
 * @author gustavonalle
 */
public final class SharedIndexCommitPolicy extends AbstractCommitPolicy {

	private final Object lock = new Object();
	private int openWriterUsers = 0;
	private boolean lastExitCloses = false;

	public SharedIndexCommitPolicy(IndexWriterHolder indexWriterHolder) {
		super( indexWriterHolder );
	}

	@Override
	public void onChangeSetApplied(boolean someFailureHappened, boolean streaming) {
		synchronized ( lock ) {
			openWriterUsers--;
			if ( openWriterUsers == 0 ) {
				if ( someFailureHappened ) {
					indexWriterHolder.forceLockRelease();
				}
				else {
					if ( ! streaming || lastExitCloses ) {
						lastExitCloses = false;
						indexWriterHolder.closeIndexWriter();
					}
				}
			}
			else {
				if ( ! someFailureHappened && ! streaming ) {
					indexWriterHolder.commitIndexWriter();
				}
			}
		}
	}

	@Override
	public void onFlush() {
		synchronized (lock) {
			if ( openWriterUsers == 0 ) {
				indexWriterHolder.closeIndexWriter();
			}
			else {
				lastExitCloses = true;
				indexWriterHolder.commitIndexWriter();
			}
		}
	}

	@Override
	public IndexWriter getIndexWriter() {
		synchronized (lock) {
			IndexWriter indexWriter = super.getIndexWriter();
			if ( indexWriter != null ) {
				openWriterUsers++;
			}
			return indexWriter;
		}
	}

	@Override
	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		synchronized ( lock ) {
			IndexWriter indexWriter = super.getIndexWriter( errorContextBuilder );
			if ( indexWriter != null ) {
				openWriterUsers++;
			}
			return indexWriter;
		}
	}
}
