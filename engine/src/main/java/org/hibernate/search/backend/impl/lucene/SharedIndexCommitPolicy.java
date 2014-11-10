/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
