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
package org.hibernate.search.store;

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
import org.slf4j.Logger;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.LoggerFactory;

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
	
	private static final Logger log = LoggerFactory.make();
	private final Timer timer = new Timer( true ); //daemon thread, the copy algorithm is robust
	
	private volatile int current;
	
	//variables having visibility granted by a read of "current"
	private FSDirectory directory;
	private String indexName;
	//get rid of it after start()
	private BuildContext context;
	private long copyChunkSize;

	//variables needed between initialize and start (used by same thread: no special care needed)
	private File sourceDir;
	private File indexDir;
	private String directoryProviderName;
	private Properties properties;
	private TriggerTask task;
	private Lock directoryProviderLock;

	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		//source guessing
		sourceDir = DirectoryProviderHelper.getSourceDirectory( directoryProviderName, properties, true );
		log.debug( "Source directory: {}", sourceDir.getPath() );
		indexDir = DirectoryProviderHelper.getVerifiedIndexDir( directoryProviderName, properties, true );
		log.debug( "Index directory: {}", indexDir.getPath() );
		try {
			indexName = indexDir.getCanonicalPath();
			directory = DirectoryProviderHelper.createFSIndex( indexDir, properties );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		copyChunkSize = DirectoryProviderHelper.getCopyBufferSize( directoryProviderName, properties );
		this.context = context;
		current = 0; //write to volatile to publish all state
	}

	public void start() {
		int currentLocal = 0;
		this.directoryProviderLock = this.context.getDirectoryProviderLock( this );
		this.context = null;
		try {
			//copy to source
			if ( new File( sourceDir, CURRENT1 ).exists() ) {
				currentLocal = 2;
			}
			else if ( new File( sourceDir, CURRENT2 ).exists() ) {
				currentLocal = 1;
			}
			else {
				log.debug( "Source directory for '{}' will be initialized", indexName);
				currentLocal = 1;
			}
			String currentString = Integer.valueOf( currentLocal ).toString();
			File subDir = new File( sourceDir, currentString );
			FileHelper.synchronize( indexDir, subDir, true, copyChunkSize );
			new File( sourceDir, CURRENT1 ).delete();
			new File( sourceDir, CURRENT2 ).delete();
			//TODO small hole, no file can be found here
			new File( sourceDir, CURRENT_DIR_NAME[currentLocal] ).createNewFile();
			log.debug( "Current directory: {}", currentLocal );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		task = new FSMasterDirectoryProvider.TriggerTask( indexDir, sourceDir );
		long period = DirectoryProviderHelper.getRefreshPeriod( properties, directoryProviderName );
		timer.scheduleAtFixedRate( task, period, period );
		this.current = currentLocal; //write to volatile to publish all state
	}

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
		if ( obj == this ) return true;
		if ( obj == null || !( obj instanceof FSMasterDirectoryProvider ) ) return false;
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

	public void stop() {
		@SuppressWarnings("unused")
		int readCurrentState = current; //Another unneeded value, to ensure visibility of state protected by memory barrier
		timer.cancel();
		task.stop();
		try {
			directory.close();
		}
		catch (Exception e) {
			log.error( "Unable to properly close Lucene directory {}" + directory.getDirectory(), e );
		}
	}

	private class TriggerTask extends TimerTask {

		private final ExecutorService executor;
		private final FSMasterDirectoryProvider.CopyDirectory copyTask;

		public TriggerTask(File source, File destination) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new FSMasterDirectoryProvider.CopyDirectory( source, destination );
		}

		public void run() {
			if ( copyTask.inProgress.compareAndSet( false, true ) ) {
				executor.execute( copyTask );
			}
			else {
				log.info( "Skipping directory synchronization, previous work still in progress: {}", indexName );
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

		public void run() {
			//TODO get rid of current and use the marker file instead?
			directoryProviderLock.lock();
			try {
				long start = System.nanoTime();//keep time after lock is acquired for correct measure
				int oldIndex = current;
				int index = oldIndex == 1 ? 2 : 1;
				File destinationFile = new File( destination, Integer.valueOf(index).toString() );
				try {
					log.trace( "Copying {} into {}", source, destinationFile );
					FileHelper.synchronize( source, destinationFile, true, copyChunkSize );
					current = index;
				}
				catch (IOException e) {
					//don't change current
					log.error( "Unable to synchronize source of " + indexName, e );
					return;
				}
				if ( ! new File( destination, CURRENT_DIR_NAME[oldIndex] ).delete() ) {
					log.warn( "Unable to remove previous marker file from source of {}", indexName );
				}
				try {
					new File( destination, CURRENT_DIR_NAME[index]  ).createNewFile();
				}
				catch( IOException e ) {
					log.warn( "Unable to create current marker in source of " + indexName, e );
				}
				log.trace( "Copy for {} took {} ms", indexName, TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) );
			}
			finally {
				directoryProviderLock.unlock();
				inProgress.set( false );
			}
		}
	}
}
