/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.reader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.LoggerFactory;

import static org.hibernate.search.reader.ReaderProviderHelper.buildMultiReader;
import static org.hibernate.search.reader.ReaderProviderHelper.clean;

/**
 * Share readers per <code>SearchFactory</code>, reusing them if they are still valid.
 * This class contains several bugs including HSEARCH-211. Since it is deprecated we are not going to
 * fix them right now.
 * 
 * @deprecated replaced by SharingBufferReaderProvider
 * @author Emmanuel Bernard
 */
@Deprecated
public class SharedReaderProvider implements ReaderProvider {
	private static final Logger log = LoggerFactory.make();

	/**
	 * nonfair lock. Need to be acquired on indexReader acquisition or release (semaphore)
	 */
	private final Lock semaphoreIndexReaderLock = new ReentrantLock();
	/**
	 * non fair list of locks to block per IndexReader only
	 * Locks have to be acquired at least for indexReader retrieval and switch
	 * ie for all activeSearchIndexReaders manipulation
	 * this map is read only after initialization, no need to synchronize
	 */
	private Map<DirectoryProvider, Lock> perDirectoryProviderManipulationLocks;
	/**
	 * Contains the active (ie non obsolete IndexReader for a given Directory
	 * There may be no entry (warm up)
	 * <p/>
	 * protected by semaphoreIndexReaderLock
	 */
	private Map<DirectoryProvider, IndexReader> activeSearchIndexReaders = new HashMap<DirectoryProvider, IndexReader>();
	/**
	 * contains the semaphore and the directory provider per IndexReader opened
	 * all read / update have to be protected by semaphoreIndexReaderLock
	 */
	private Map<IndexReader, ReaderData> searchIndexReaderSemaphores = new HashMap<IndexReader, ReaderData>();

	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		boolean trace = log.isTraceEnabled();
		int length = directoryProviders.length;
		IndexReader[] readers = new IndexReader[length];
		if ( trace ) {
			log.trace( "Opening IndexReader for directoryProviders: {}", length );
		}

		for ( int index = 0; index < length; index++ ) {
			DirectoryProvider directoryProvider = directoryProviders[index];
			IndexReader reader;
			Lock directoryProviderLock = perDirectoryProviderManipulationLocks.get( directoryProvider );
			if ( trace ) {
				log.trace( "Opening IndexReader from {}", directoryProvider.getDirectory() );
			}
			directoryProviderLock.lock(); //needed for same problem as the double-checked locking
			try {
				reader = activeSearchIndexReaders.get( directoryProvider );
			}
			finally {
				directoryProviderLock.unlock();
			}
			if ( reader == null ) {
				if ( trace ) {
					log.trace( "No shared IndexReader, opening a new one: {}", directoryProvider.getDirectory() );
				}
				reader = replaceActiveReader( null, directoryProviderLock, directoryProvider, readers );
			}
			else {
				boolean isCurrent;
				try {
					isCurrent = reader.isCurrent();
				}
				catch ( IOException e ) {
					throw new SearchException( "Unable to read current status of Lucene IndexReader", e );
				}
				if ( !isCurrent ) {
					if ( trace ) {
						log.trace(
								"Out of date shared IndexReader found, opening a new one: {}",
								directoryProvider.getDirectory()
						);
					}
					IndexReader outOfDateReader = reader;
					reader = replaceActiveReader( outOfDateReader, directoryProviderLock, directoryProvider, readers );
				}
				else {
					if ( trace ) {
						log.trace( "Valid shared IndexReader: {}" + directoryProvider.getDirectory() );
					}
					directoryProviderLock.lock();
					try {
						//read the latest active one, the current one could be out of date and closed already
						//the latest active is guaranteed to be active because it's protected by the dp lock
						reader = activeSearchIndexReaders.get( directoryProvider );
						semaphoreIndexReaderLock.lock();
						try {
							SharedReaderProvider.ReaderData readerData = searchIndexReaderSemaphores.get( reader );
							//TODO if readerData is null????
							readerData.semaphore++;
							searchIndexReaderSemaphores.put( reader, readerData ); //not necessary
							if ( trace ) {
								log.trace( "Semaphore increased: {} for {}", readerData.semaphore, reader );
							}
						}
						finally {
							semaphoreIndexReaderLock.unlock();
						}
					}
					finally {
						directoryProviderLock.unlock();
					}
				}
			}
			readers[index] = reader;
		}
		return buildMultiReader( length, readers );
	}

	@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
	private IndexReader replaceActiveReader(IndexReader outOfDateReader, Lock directoryProviderLock, DirectoryProvider directoryProvider, IndexReader[] readers) {
		boolean trace = log.isTraceEnabled();
		IndexReader oldReader;
		boolean closeOldReader = false;
		boolean closeOutOfDateReader = false;
		IndexReader reader;
		/**
		 * Since out of lock protection, can have multiple readers created in //
		 * not worse than NotShared and limit the locking time, hence scalability
		 */
		try {
			reader = IndexReader.open( directoryProvider.getDirectory(), true );
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to open Lucene IndexReader", e );
		}
		directoryProviderLock.lock();
		try {
			//since not protected by a lock, other ones can have been added
			oldReader = activeSearchIndexReaders.put( directoryProvider, reader );
			semaphoreIndexReaderLock.lock();
			try {
				searchIndexReaderSemaphores.put( reader, new ReaderData( 1, directoryProvider ) );
				if ( trace ) {
					log.trace( "Semaphore: 1 for {}", reader );
				}
				if ( outOfDateReader != null ) {
					ReaderData readerData = searchIndexReaderSemaphores.get( outOfDateReader );
					if ( readerData == null ) {
						closeOutOfDateReader = false; //already removed by another previous thread
					}
					else if ( readerData.semaphore == 0 ) {
						searchIndexReaderSemaphores.remove( outOfDateReader );
						closeOutOfDateReader = true;
					}
					else {
						closeOutOfDateReader = false;
					}
				}

				if ( oldReader != null && oldReader != outOfDateReader ) {
					ReaderData readerData = searchIndexReaderSemaphores.get( oldReader );
					if ( readerData == null ) {
						log.warn( "Semaphore should not be null" );
						closeOldReader = true; //TODO should be true or false?
					}
					else if ( readerData.semaphore == 0 ) {
						searchIndexReaderSemaphores.remove( oldReader );
						closeOldReader = true;
					}
					else {
						closeOldReader = false;
					}
				}
			}
			finally {
				semaphoreIndexReaderLock.unlock();
			}
		}
		finally {
			directoryProviderLock.unlock();
		}
		if ( closeOutOfDateReader ) {
			if ( trace ) {
				log.trace( "Closing out of date IndexReader {}", outOfDateReader );
			}
			try {
				outOfDateReader.close();
			}
			catch ( IOException e ) {
				clean( new SearchException( "Unable to close Lucene IndexReader", e ), readers );
			}
		}
		if ( closeOldReader ) {
			if ( trace ) {
				log.trace( "Closing old IndexReader {}", oldReader );
			}
			try {
				oldReader.close();
			}
			catch ( IOException e ) {
				clean( new SearchException( "Unable to close Lucene IndexReader", e ), readers );
			}
		}
		return reader;
	}

	public void closeReader(IndexReader reader) {
		boolean trace = log.isTraceEnabled();
		if ( reader == null ) {
			return;
		}
		IndexReader[] readers;
		//TODO should it be CacheableMultiReader? Probably no
		if ( reader instanceof MultiReader ) {
			readers = ReaderProviderHelper.getSubReadersFromMultiReader( ( MultiReader ) reader );
			if ( trace ) {
				log.trace( "Closing MultiReader: {}", reader );
			}
		}
		else {
			throw new AssertionFailure( "Everything should be wrapped in a MultiReader" );
		}

		for ( IndexReader subReader : readers ) {
			closeInternalReader( trace, subReader, false );
		}
	}

	private void closeInternalReader(boolean trace, IndexReader subReader, boolean finalClose) {
		ReaderData readerData;
		//TODO can we avoid that lock?
		semaphoreIndexReaderLock.lock();
		try {
			readerData = searchIndexReaderSemaphores.get( subReader );
		}
		finally {
			semaphoreIndexReaderLock.unlock();
		}

		if ( readerData == null ) {
			log.error( "Trying to close a Lucene IndexReader not present: {}", subReader.directory() );
			//TODO should we try to close?
			return;
		}

		//acquire the locks in the same order as everywhere else
		Lock directoryProviderLock = perDirectoryProviderManipulationLocks.get( readerData.provider );
		boolean closeReader = false;
		directoryProviderLock.lock();
		try {
			boolean isActive;
			isActive = activeSearchIndexReaders.get( readerData.provider ) == subReader;
			if ( trace ) {
				log.trace( "Indexreader not active: {}", subReader );
			}
			semaphoreIndexReaderLock.lock();
			try {
				readerData = searchIndexReaderSemaphores.get( subReader );
				if ( readerData == null ) {
					log.error( "Trying to close a Lucene IndexReader not present: {}" + subReader.directory() );
					//TODO should we try to close?
					return;
				}

				//final close, the semaphore should be at 0 already
				if ( !finalClose ) {
					readerData.semaphore--;
					if ( trace ) {
						log.trace( "Semaphore decreased to: {} for {}", readerData.semaphore, subReader );
					}
				}

				if ( readerData.semaphore < 0 ) {
					log.error( "Semaphore negative: {}", subReader.directory() );
				}
				if ( ( !isActive ) && readerData.semaphore == 0 ) {
					searchIndexReaderSemaphores.remove( subReader );
					closeReader = true;
				}
				else {
					closeReader = false;
				}
			}
			finally {
				semaphoreIndexReaderLock.unlock();
			}
		}
		finally {
			directoryProviderLock.unlock();
		}

		if ( closeReader ) {
			if ( trace ) {
				log.trace( "Closing IndexReader: {}", subReader );
			}
			try {
				subReader.close();
			}
			catch ( IOException e ) {
				log.warn( "Unable to close Lucene IndexReader", e );
			}
		}
	}

	public void initialize(Properties props, BuildContext context) {
		Set<DirectoryProvider<?>> providers = context.getDirectoryProviders();
		perDirectoryProviderManipulationLocks = new HashMap<DirectoryProvider, Lock>( providers.size() );
		for ( DirectoryProvider dp : providers ) {
			perDirectoryProviderManipulationLocks.put( dp, new ReentrantLock() );
		}
		perDirectoryProviderManipulationLocks = Collections.unmodifiableMap( perDirectoryProviderManipulationLocks );
	}

	public void destroy() {
		boolean trace = log.isTraceEnabled();
		IndexReader[] readers;
		semaphoreIndexReaderLock.lock();
		try {
			//release active readers
			activeSearchIndexReaders.clear();
			readers = searchIndexReaderSemaphores.keySet()
					.toArray( new IndexReader[searchIndexReaderSemaphores.size()] );
		}
		finally {
			semaphoreIndexReaderLock.unlock();
		}

		for ( IndexReader reader : readers ) {
			closeInternalReader( trace, reader, true );
		}

		if ( searchIndexReaderSemaphores.size() != 0 ) {
			log.warn( "ReaderProvider contains readers not properly closed at destroy time" );
		}

	}

	private static class ReaderData {

		public ReaderData(int semaphore, DirectoryProvider provider) {
			this.semaphore = semaphore;
			this.provider = provider;
		}

		public int semaphore;
		public DirectoryProvider provider;
	}
}
