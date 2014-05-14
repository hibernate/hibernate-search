/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class SharedIndexWorkspaceImpl extends AbstractWorkspaceImpl {

	private final Object lock = new Object();
	private int openWriterUsers = 0;
	private boolean lastExitCloses = false;

	public SharedIndexWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		super( indexManager, context, cfg );
	}

	@Override
	public void afterTransactionApplied(boolean someFailureHappened, boolean streaming) {
		synchronized ( lock ) {
			openWriterUsers--;
			if ( openWriterUsers == 0 ) {
				if ( someFailureHappened ) {
					writerHolder.forceLockRelease();
				}
				else {
					if ( ! streaming || lastExitCloses ) {
						lastExitCloses = false;
						writerHolder.closeIndexWriter();
					}
				}
			}
			else {
				if ( ! someFailureHappened && ! streaming ) {
					writerHolder.commitIndexWriter();
				}
			}
		}
	}

	@Override
	public IndexWriter getIndexWriter() {
		synchronized ( lock ) {
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

	@Override
	public void flush() {
		synchronized ( lock ) {
			if ( openWriterUsers == 0 ) {
				writerHolder.closeIndexWriter();
			}
			else {
				lastExitCloses = true;
				writerHolder.commitIndexWriter();
			}
		}
	}

	@Override
	public void notifyWorkApplied(LuceneWork work) {
		incrementModificationCounter();
	}

}
