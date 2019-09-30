/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegatorImpl;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexAccessor implements AutoCloseable {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final DirectoryHolder directoryHolder;
	private final IndexWriterDelegatorImpl indexWriterDelegator;
	private final EventContext indexEventContext;

	public IndexAccessor(DirectoryHolder directoryHolder, Analyzer analyzer,
			ErrorHandler errorHandler, EventContext indexEventContext) {
		this.directoryHolder = directoryHolder;
		this.indexWriterDelegator = new IndexWriterDelegatorImpl(
				directoryHolder, analyzer, errorHandler, indexEventContext
		);
		this.indexEventContext = indexEventContext;
	}

	public EventContext getIndexEventContext() {
		return indexEventContext;
	}

	public void start() {
		try {
			directoryHolder.start();
			indexWriterDelegator.ensureIndexExists();
		}
		catch (IOException | RuntimeException e) {
			new SuppressingCloser( e ).push( directoryHolder );
			throw log.unableToInitializeIndexDirectory(
					e.getMessage(),
					indexEventContext,
					e
			);
		}
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( IndexWriterDelegatorImpl::close, indexWriterDelegator );
			closer.push( DirectoryHolder::close, directoryHolder );
		}
	}

	public IndexWriterDelegator getIndexWriterDelegator() {
		return indexWriterDelegator;
	}

	/**
	 * Opens an IndexReader having visibility on uncommitted writes from
	 * the IndexWriter, if any writer is open, or null if no IndexWriter is open.
	 * @param applyDeletes Applying deletes is expensive, say no if you can deal with stale hits during queries
	 * @return a new NRT IndexReader if an IndexWriter is available, or <code>null</code> otherwise
	 */
	public DirectoryReader openNRTIndexReader(boolean applyDeletes) throws IOException {
		final IndexWriter indexWriter = indexWriterDelegator.getIndexWriterOrNull();
		if ( indexWriter != null ) {
			// TODO HSEARCH-3117 should parameter writeAllDeletes take the same value as applyDeletes?
			return DirectoryReader.open( indexWriter, applyDeletes, applyDeletes );
		}
		else {
			return null;
		}
	}

	/**
	 * Opens an IndexReader from the Directory (not using the IndexWriter)
	 */
	public DirectoryReader openDirectoryIndexReader() throws IOException {
		return DirectoryReader.open( directoryHolder.get() );
	}

	public Directory getDirectoryForTests() {
		return directoryHolder.get();
	}
}
