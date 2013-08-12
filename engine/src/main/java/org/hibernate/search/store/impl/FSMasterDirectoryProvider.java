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
package org.hibernate.search.store.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.store.FSDirectory;

import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * File based DirectoryProvider that takes care of index copy
 * The base directory is represented by hibernate.search.<index>.indexBase
 * The index is created in <base directory>/<index name>
 * The source (aka copy) directory is built from <sourceBase>/<index name>
 *
 * A copy is triggered every refresh seconds
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
//TODO rename copy?
public class FSMasterDirectoryProvider implements DirectoryProvider<FSDirectory> {

	private static final String CURRENT1 = "current1";
	private static final String CURRENT2 = "current2";
	// defined to have CURRENT_DIR_NAME[1] == "current"+"1":
	private static final String[] CURRENT_DIR_NAME = { null, CURRENT1, CURRENT2 };

	private static final Log log = LoggerFactory.make();
	private final Timer timer = new Timer( true ); //daemon thread, the copy algorithm is robust

	private volatile int current;

	//variables having visibility granted by a read of "current"
	private FSDirectory directory;
	private String indexName;
	private long copyChunkSize;

	//variables needed between initialize and start (used by same thread: no special care needed)
	private File sourceDir;
	private File indexDir;
	private String directoryProviderName;
	private Properties properties;
	private TriggerTask task;
	private Lock directoryProviderLock;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		//source guessing
		sourceDir = DirectoryProviderHelper.getSourceDirectory( directoryProviderName, properties, true );
		log.debugf( "Source directory: %s", sourceDir.getPath() );
		indexDir = DirectoryProviderHelper.getVerifiedIndexDir( directoryProviderName, properties, true );
		log.debugf( "Index directory: %s", indexDir.getPath() );
		try {
			indexName = indexDir.getCanonicalPath();
			directory = DirectoryProviderHelper.createFSIndex( indexDir, properties );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		copyChunkSize = DirectoryProviderHelper.getCopyBufferSize( directoryProviderName, properties );
		current = 0; //write to volatile to publish all state
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		int currentLocal = 0;
		this.directoryProviderLock = indexManager.getDirectoryModificationLock();
		try {
			//copy to source
			if ( new File( sourceDir, CURRENT1 ).exists() ) {
				currentLocal = 2;
			}
			else if ( new File( sourceDir, CURRENT2 ).exists() ) {
				currentLocal = 1;
			}
			else {
				log.debugf( "Source directory for '%s' will be initialized", indexName);
				currentLocal = 1;
			}
			String currentString = Integer.valueOf( currentLocal ).toString();
			File subDir = new File( sourceDir, currentString );
			FileHelper.synchronize( indexDir, subDir, true, copyChunkSize );
			new File( sourceDir, CURRENT1 ).delete();
			new File( sourceDir, CURRENT2 ).delete();
			//TODO small hole, no file can be found here
			new File( sourceDir, CURRENT_DIR_NAME[currentLocal] ).createNewFile();
			log.debugf( "Current directory: %d", currentLocal );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		task = new FSMasterDirectoryProvider.TriggerTask( indexDir, sourceDir );
		long period = DirectoryProviderHelper.getRefreshPeriod( properties, directoryProviderName );
		timer.scheduleAtFixedRate( task, period, period );
		this.current = currentLocal; //write to volatile to publish all state
	}

	@Override
	public FSDirectory getDirectory() {
		@SuppressWarnings("unused")
		int readCurrentState = current; //Unneeded value, needed to ensure visibility of state protected by memory barrier
		return directory;
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || !( obj instanceof FSMasterDirectoryProvider ) ) {
			return false;
		}
		FSMasterDirectoryProvider other = (FSMasterDirectoryProvider)obj;
		//break both memory barriers by reading volatile variables:
		@SuppressWarnings("unused")
		int readCurrentState = other.current;
		readCurrentState = this.current;
		return indexName.equals( other.indexName );
	}

	@Override
	public int hashCode() {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		@SuppressWarnings("unused")
		int readCurrentState = current; //Unneeded value, to ensure visibility of state protected by memory barrier
		int hash = 11;
		return 37 * hash + indexName.hashCode();
	}

	@Override
	public void stop() {
		@SuppressWarnings("unused")
		int readCurrentState = current; //Another unneeded value, to ensure visibility of state protected by memory barrier
		timer.cancel();
		task.stop();
		try {
			directory.close();
		}
		catch (Exception e) {
			log.unableToCloseLuceneDirectory( directory.getDirectory(), e );
		}
	}

	private class TriggerTask extends TimerTask {

		private final ExecutorService executor;
		private final FSMasterDirectoryProvider.CopyDirectory copyTask;

		public TriggerTask(File source, File destination) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new FSMasterDirectoryProvider.CopyDirectory( source, destination );
		}

		@Override
		public void run() {
			if ( copyTask.inProgress.compareAndSet( false, true ) ) {
				executor.execute( copyTask );
			}
			else {
				log.skippingDirectorySynchronization( indexName );
			}
		}

		public void stop() {
			executor.shutdownNow();
		}
	}

	private class CopyDirectory implements Runnable {
		private final File source;
		private final File destination;
		private final AtomicBoolean inProgress = new AtomicBoolean( false );

		public CopyDirectory(File source, File destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public void run() {
			//TODO get rid of current and use the marker file instead?
			directoryProviderLock.lock();
			try {
				long start = System.nanoTime();//keep time after lock is acquired for correct measure
				int oldIndex = current;
				int index = oldIndex == 1 ? 2 : 1;
				File destinationFile = new File( destination, Integer.valueOf( index ).toString() );
				try {
					log.tracef( "Copying %s into %s", source, destinationFile );
					FileHelper.synchronize( source, destinationFile, true, copyChunkSize );
					current = index;
				}
				catch (IOException e) {
					//don't change current
					log.unableToSynchronizeSource( indexName, e );
					return;
				}
				if ( ! new File( destination, CURRENT_DIR_NAME[oldIndex] ).delete() ) {
					log.unableToRemovePreviousMarket( indexName );
				}
				try {
					new File( destination, CURRENT_DIR_NAME[index] ).createNewFile();
				}
				catch (IOException e) {
					log.unableToCreateCurrentMarker( indexName, e );
				}
				log.tracef( "Copy for %s took %d ms", indexName, TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) );
			}
			finally {
				directoryProviderLock.unlock();
				inProgress.set( false );
			}
		}
	}
}
