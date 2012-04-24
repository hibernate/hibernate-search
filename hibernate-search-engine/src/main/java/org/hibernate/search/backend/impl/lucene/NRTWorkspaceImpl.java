/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.backend.impl.lucene;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The Workspace implementation to be used to take advantage of NRT Lucene features.
 * IndexReader instances are obtained directly from the IndexWriter, which is not forced
 * to flush all pending changes to the Directory structure.
 * 
 * We keep a reference Reader, obtained from the IndexWriter each time a transactional queue
 * is applied, so that the IndexReader instance "sees" only fully committed transactions;
 * the reference is never returned to clients, but each time a client needs an IndexReader
 * a clone is created from the last refreshed IndexReader.
 * 
 * Since the backend is forced to create a reference IndexReader after each (skipped) commit,
 * some IndexReaders might be opened without being ever used.
 * 
 * This class implements both Workspace and ReaderProvider.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class NRTWorkspaceImpl extends AbstractWorkspaceImpl implements DirectoryBasedReaderProvider {

	private static final Log log = LoggerFactory.make();

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final ReadLock readLock = readWriteLock.readLock();
	private final WriteLock writeLock = readWriteLock.writeLock();
	private final String indexName;

	//guardedBy readLock/writeLok
	private IndexReader currentReferenceReader = null;

	public NRTWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext buildContext, Properties cfg) {
		super( indexManager, buildContext, cfg );
		indexName = indexManager.getIndexName();
	}

	@Override
	public void afterTransactionApplied(boolean someFailureHappened, boolean streaming) {
		if ( someFailureHappened ) {
			writerHolder.forceLockRelease();
		}
		else {
			if ( ! streaming ) {
				flush();
			}
		}
	}

	@Override
	public IndexReader openIndexReader() {
		// we need to readLock to read the state of the currentReferenceReader
		readLock.lock(); // balanced by the finally block
		boolean readlockAcquired = true;
		try {
			if ( currentReferenceReader == null ) {
				readLock.unlock();
				// in this case we need to create a new Reader, so we need to upgrade
				// the read lock to the write lock (upgrade is not supported by a
				// ReentrantReadWriteLock so we need to release the readlock first)
				readlockAcquired = false;
				writeLock.lock(); // balanced by the inner final block
				try {
					// check again as we had to release the lock after the first check:
					if ( currentReferenceReader == null) {
						currentReferenceReader = writerHolder.openDirectoryIndexReader();
					}
				}
				finally {
					writeLock.unlock();
				}
				readLock.lock();
				// if we succeed in acquiring this, make sure we will release it again:
				readlockAcquired = true;
			}
			return cloneReader( currentReferenceReader );
		}
		finally {
			if ( readlockAcquired ) {
				readLock.unlock();
			}
		}
	}

	/**
	 * We need to return clones so that each reader can be closed independently;
	 * clones should share most heavy-weight buffers anyway.
	 */
	private IndexReader cloneReader(IndexReader indexReader) {
		try {
			return indexReader.clone( true );
		}
		catch ( CorruptIndexException cie ) {
			throw log.cantOpenCorruptedIndex( cie, indexName );
		}
		catch ( IOException ioe ) {
			throw log.ioExceptionOnIndex( ioe, indexName );
		}
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		if ( reader == null ) {
			return;
		}
		try {
			reader.close();
		}
		catch ( IOException e ) {
			log.unableToCLoseLuceneIndexReader( e );
		}
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
	}

	@Override
	public void stop() {
		writeLock.lock();
		try {
			closeIndexReader( currentReferenceReader );
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void flush() {
		IndexReader newIndexReader = writerHolder.openNRTIndexReader( true );
		writeLock.lock();
		IndexReader oldReader = currentReferenceReader;
		currentReferenceReader = newIndexReader;
		writeLock.unlock();
		try {
			if ( oldReader != null ) {
				oldReader.close();
			}
		}
		catch ( IOException e ) {
			log.unableToCLoseLuceneIndexReader( e );
		}
	}

}
