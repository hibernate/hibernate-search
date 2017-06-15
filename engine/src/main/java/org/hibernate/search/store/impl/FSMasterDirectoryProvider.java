/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.spi.DirectoryHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * File based DirectoryProvider that takes care of index copy
 * The base directory is represented by hibernate.search.{@literal <index>}.indexBase
 * The index is created in {@literal <base directory>/<index name>}
 * The source (aka copy) directory is built from {@literal <sourceBase>/<index name>}
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

	//variables needed between initialize and start (used by same thread: no special care needed)
	private Path sourceDir;
	private Path indexDir;
	private String directoryProviderName;
	private Properties properties;
	private TriggerTask task;
	private Lock directoryProviderLock;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		//source guessing
		sourceDir = DirectoryProviderHelper.getSourceDirectoryPath( directoryProviderName, properties, true );
		log.debugf( "Source directory: %s", sourceDir );
		indexDir = DirectoryHelper.getVerifiedIndexPath( directoryProviderName, properties, true ).normalize();
		log.debugf( "Index directory: %s", indexDir );
		try {
			directory = DirectoryProviderHelper.createFSIndex( indexDir, properties, context.getServiceManager() );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		//No longer useful but invoke it still to log a deprecation warning as needed:
		DirectoryProviderHelper.getCopyBufferSize( directoryProviderName, properties );
		current = 0; //write to volatile to publish all state
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		int currentLocal = 0;
		this.directoryProviderLock = indexManager.getDirectoryModificationLock();
		try {
			//copy to source
			if ( Files.exists( sourceDir.resolve( CURRENT1 ) ) ) {
				currentLocal = 2;
			}
			else if ( Files.exists( sourceDir.resolve( CURRENT2 ) ) ) {
				currentLocal = 1;
			}
			else {
				log.debugf( "Source directory for '%s' will be initialized", indexDir.toString() );
				currentLocal = 1;
			}
			String currentString = Integer.valueOf( currentLocal ).toString();
			Path subDir = sourceDir.resolve( currentString );
			FileHelper.synchronize( indexDir, subDir, true );
			Files.deleteIfExists( sourceDir.resolve( CURRENT1 ) );
			Files.deleteIfExists( sourceDir.resolve( CURRENT2 ) );
			//TODO small hole, no file can be found here
			Files.createFile( sourceDir.resolve( CURRENT_DIR_NAME[currentLocal] ) );
			log.debugf( "Current directory: %d", (Integer) currentLocal );
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
		throw new AssertionFailure( "this type can not be compared reliably" );
	}

	@Override
	public int hashCode() {
		throw new AssertionFailure( "this type can not be compared reliably" );
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

		public TriggerTask(Path source, Path destination) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new FSMasterDirectoryProvider.CopyDirectory( source, destination );
		}

		@Override
		public void run() {
			if ( copyTask.inProgress.compareAndSet( false, true ) ) {
				executor.execute( copyTask );
			}
			else {
				log.skippingDirectorySynchronization( indexDir.toString() );
			}
		}

		public void stop() {
			executor.shutdownNow();
		}
	}

	private class CopyDirectory implements Runnable {
		private final Path source;
		private final Path destination;
		private final AtomicBoolean inProgress = new AtomicBoolean( false );

		public CopyDirectory(Path source, Path destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public void run() {
			//TODO get rid of current and use the marker file instead?
			directoryProviderLock.lock();
			final boolean traceEnabled = log.isTraceEnabled();
			try {
				long startTime = 0;
				if ( traceEnabled ) {
					startTime = System.nanoTime();//keep time after lock is acquired for correct measure
				}
				int oldIndex = current;
				int index = oldIndex == 1 ? 2 : 1;
				Path destinationFile = destination.resolve( Integer.valueOf( index ).toString() );
				try {
					log.tracef( "Copying %s into %s", source, destinationFile );
					FileHelper.synchronize( source, destinationFile, true );
					current = index;
				}
				catch (IOException e) {
					//don't change current
					log.unableToSynchronizeSource( indexDir.toString(), e );
					return;
				}
				try {
					Files.delete( destination.resolve( CURRENT_DIR_NAME[oldIndex] ) );
				}
				catch (IOException e) {
					log.unableToRemovePreviousMarker( indexDir.toString(), e );
				}
				try {
					Files.createFile( destination.resolve( CURRENT_DIR_NAME[index] ) );
				}
				catch (IOException e) {
					log.unableToCreateCurrentMarker( indexDir.toString(), e );
				}
				if ( traceEnabled ) {
					log.tracef( "Copy for %s took %d ms", indexDir.toString(), TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - startTime ) );
				}
			}
			finally {
				directoryProviderLock.unlock();
				inProgress.set( false );
			}
		}
	}
}
