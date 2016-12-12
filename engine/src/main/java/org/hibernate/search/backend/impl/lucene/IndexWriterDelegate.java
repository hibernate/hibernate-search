/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.ScopedLuceneAnalyzer;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.backend.impl.lucene.analysis.ConcurrentlyMutableAnalyzer;

/**
 * Encapsulates various operations to be performed on a single IndexWriter.
 * Avoid using {@link org.hibernate.search.store.Workspace#getIndexWriter()} directly as it bypasses lifecycle
 * management of the IndexWriter such as reference counting, potentially leading to leaks.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public final class IndexWriterDelegate {

	private final IndexWriter indexWriter;
	private final ConcurrentlyMutableAnalyzer mutableAnalyzer;
	private final Lock readLock;
	private final Lock writeLock;

	public IndexWriterDelegate(final IndexWriter indexWriter) {
		this.indexWriter = indexWriter;
		this.mutableAnalyzer = (ConcurrentlyMutableAnalyzer) indexWriter.getAnalyzer();
		ReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}

	public void deleteDocuments(final Query termDeleteQuery) throws IOException {
		indexWriter.deleteDocuments( termDeleteQuery );
	}

	public void deleteDocuments(final Term idTerm) throws IOException {
		indexWriter.deleteDocuments( idTerm );
	}

	public void addDocument(final Document document, final ScopedAnalyzerReference analyzerReference) throws IOException {
		//This is now equivalent to the old "addDocument" method:
		updateDocument( null, document, analyzerReference );
	}

	public void updateDocument(final Term idTerm, final Document document, final ScopedAnalyzerReference analyzerReference) throws IOException {
		// Try being optimistic first:
		ScopedLuceneAnalyzer scopedAnalyzer = (ScopedLuceneAnalyzer) analyzerReference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
		final boolean applyWithinReadLock;
		readLock.lock();
		try {
			applyWithinReadLock = mutableAnalyzer.isCompatibleWith( scopedAnalyzer );
			if ( applyWithinReadLock ) {
				indexWriter.updateDocument( idTerm, document );
			}
		}
		finally {
			readLock.unlock();
		}
		// If that failed, take the pessimistic lock:
		if ( ! applyWithinReadLock ) {
			writeLock.lock();
			try {
				mutableAnalyzer.updateAnalyzer( scopedAnalyzer );
				indexWriter.updateDocument( idTerm, document );
			}
			finally {
				writeLock.unlock();
			}
		}
	}

	/**
	 * This method should not be used: created only to avoid changes in public API.
	 * @deprecated
	 * @return the {@link IndexWriter}
	 */
	@Deprecated
	public IndexWriter getIndexWriter() {
		return indexWriter;
	}

}
