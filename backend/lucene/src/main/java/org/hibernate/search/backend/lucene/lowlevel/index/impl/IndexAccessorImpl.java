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
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegatorImpl;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SleepingLockWrapper;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexAccessorImpl implements AutoCloseable, IndexAccessor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;
	private final DirectoryHolder directoryHolder;
	private final IndexWriterProvider indexWriterProvider;
	private final IndexReaderProvider indexReaderProvider;

	public IndexAccessorImpl(EventContext eventContext, DirectoryHolder directoryHolder,
			IndexWriterProvider indexWriterProvider, IndexReaderProvider indexReaderProvider) {
		this.eventContext = eventContext;
		this.directoryHolder = directoryHolder;
		this.indexWriterProvider = indexWriterProvider;
		this.indexReaderProvider = indexReaderProvider;
	}

	public void start() throws IOException {
		directoryHolder.start();
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( IndexReaderProvider::clear, indexReaderProvider );
			closer.push( IndexWriterProvider::clear, indexWriterProvider );
			closer.push( DirectoryHolder::close, directoryHolder );
		}
	}

	@Override
	public void reset() throws IOException {
		log.indexAccessorReset( eventContext );
		indexWriterProvider.clear();
		indexReaderProvider.clear();
	}

	@Override
	public void ensureIndexExists() {
		try {
			Directory directory = directoryHolder.get();

			if ( DirectoryReader.indexExists( directory ) ) {
				return;
			}

			initializeDirectory( directory );
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToInitializeIndexDirectory(
					e.getMessage(), eventContext, e
			);
		}
	}

	@Override
	public void commit() {
		try {
			IndexWriterDelegatorImpl delegator = indexWriterProvider.getOrNull();
			if ( delegator != null ) {
				delegator.commit();
			}
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCommitIndex( eventContext, e );
		}
	}

	@Override
	public void refresh() {
		try {
			indexReaderProvider.refresh();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToRefreshIndex( eventContext, e );
		}
	}

	@Override
	public IndexWriterDelegator getIndexWriterDelegator() throws IOException {
		return indexWriterProvider.getOrCreate();
	}

	@Override
	public DirectoryReader getIndexReader() throws IOException {
		return indexReaderProvider.getOrCreate();
	}

	public Directory getDirectoryForTests() {
		return directoryHolder.get();
	}

	private void initializeDirectory(Directory directory) throws IOException {
		try {
			IndexWriterConfig iwriterConfig = new IndexWriterConfig( AnalyzerConstants.KEYWORD_ANALYZER )
					.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
			//Needs to have a timeout higher than zero to prevent race conditions over (network) RPCs
			//for distributed indexes (Infinispan but probably also NFS and similar)
			SleepingLockWrapper delayedDirectory = new SleepingLockWrapper( directory, 2000, 20 );
			IndexWriter iw = new IndexWriter( delayedDirectory, iwriterConfig );
			iw.close();
		}
		catch (LockObtainFailedException lofe) {
			log.lockingFailureDuringInitialization( directory.toString(), eventContext );
		}
	}

}
