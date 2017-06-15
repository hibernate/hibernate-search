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

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.spi.DirectoryHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * File based directory provider that takes care of getting a version of the index
 * from a given source.
 * The base directory is represented by hibernate.search.{@literal <index>}.indexBase
 * The index is created in {@literal <base directory>/<index name>}
 * The source (aka copy) directory is built from {@literal <sourceBase>/<index name>}
 * <p>
 * A copy is triggered every refresh seconds
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Oliver Siegmar
 */
public class FSSlaveDirectoryProvider implements DirectoryProvider<Directory> {

	private static final Log log = LoggerFactory.make();
	private final Timer timer = new Timer( true ); //daemon thread, the copy algorithm is robust

	private volatile boolean initialized = false;
	private volatile boolean started = false;

	private volatile int current; //used also as memory barrier of all other values, which are set once.

	//variables having visibility granted by a read of "current"
	private volatile Directory dummyDirectory;
	private FSDirectory directory1;
	private FSDirectory directory2;

	//variables needed between initialize and start (used by same thread: no special care needed)
	private Path sourceIndexDir;
	private Path indexDir;
	private String directoryProviderName;
	private Properties properties;
	private UpdateTask updateTask;
	private ServiceManager serviceManager;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		this.serviceManager = context.getServiceManager();
		//source guessing
		sourceIndexDir = DirectoryProviderHelper.getSourceDirectoryPath( directoryProviderName, properties, false );
		log.debugf( "Source directory: %s", sourceIndexDir );
		indexDir = DirectoryHelper.getVerifiedIndexPath( directoryProviderName, properties, true ).normalize();
		log.debugf( "Index directory: %s", indexDir );
		//No longer useful but invoke it still to log a deprecation warning as needed:
		DirectoryProviderHelper.getCopyBufferSize( directoryProviderName, properties );
		current = 0; //publish all state to other threads
	}

	private boolean currentMarkerIsInSource() {
		int retry = ConfigurationParseHelper.getIntValue( properties, Environment.RETRY_MARKER_LOOKUP, 0 );
		if ( retry < 0 ) {
			throw new SearchException( Environment.RETRY_MARKER_LOOKUP +
					" option must be a positive integer, but was \"" + retry + "\"" );
		}
		boolean currentMarkerInSource = false;
		for ( int tried = 0 ; tried <= retry ; tried++ ) {
			//we try right away the first time
			if ( tried > 0 ) {
				try {
					Thread.sleep( TimeUnit.SECONDS.toMillis( 5 ) );
				}
				catch (InterruptedException e) {
					//continue
					Thread.currentThread().interrupt();
				}
			}
			currentMarkerInSource =
					Files.exists( sourceIndexDir.resolve( "current1" ) )
					|| Files.exists( sourceIndexDir.resolve( "current2" ) );
			if ( currentMarkerInSource ) {
				break;
			}
		}
		return currentMarkerInSource;
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		if ( ! attemptInitializeAndStart() ) {
			// if we failed to initialize and/or start, we'll try again later: setup a timer
			long period = DirectoryProviderHelper.getRetryInitializePeriod( properties, directoryProviderName );
			if ( period != 0 ) {
				scheduleTask( new InitTask(), period );
			}
			else {
				throw new SearchException( "Failed to initialize DirectoryProvider \""
						+ directoryProviderName + "\": could not find marker file in index source" );
			}
		}
	}

	private void startIt() {
		@SuppressWarnings("unused")
		int readCurrentState = current; //Unneeded value, but ensure visibility of state protected by memory barrier
		int currentToBe = 0;
		try {
			directory1 = DirectoryProviderHelper.createFSIndex( indexDir.resolve( "1" ), properties, serviceManager );
			directory2 = DirectoryProviderHelper.createFSIndex( indexDir.resolve( "2" ), properties, serviceManager );
			Path currentMarker = indexDir.resolve( "current1" );
			Path current2Marker = indexDir.resolve( "current2" );
			if ( Files.exists( currentMarker ) ) {
				currentToBe = 1;
				Files.deleteIfExists( current2Marker ); //TODO or throw an exception?
			}
			else if ( Files.exists( current2Marker ) ) {
				currentToBe = 2;
			}
			else {
				//no default
				log.debug( "Setting directory 1 as current" );
				currentToBe = 1;
				Path destinationFile = indexDir.resolve( Integer.valueOf( currentToBe ).toString() );
				int sourceCurrent;
				if ( Files.exists( sourceIndexDir.resolve( "current1" ) ) ) {
					sourceCurrent = 1;
				}
				else if ( Files.exists( sourceIndexDir.resolve( "current2" ) ) ) {
					sourceCurrent = 2;
				}
				else {
					throw new SearchException( "No current file marker found in source directory: " + sourceIndexDir );
				}
				try {
					FileHelper.synchronize(
							sourceIndexDir.resolve( String.valueOf( sourceCurrent ) ),
							destinationFile, true
					);
				}
				catch (IOException e) {
					throw new SearchException( "Unable to synchronize directory: " + indexDir.toString(), e );
				}
				Files.createFile( currentMarker );
			}
			log.debugf( "Current directory: %d", (Integer) currentToBe );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		updateTask = new UpdateTask( sourceIndexDir, indexDir );
		long period = DirectoryProviderHelper.getRefreshPeriod( properties, directoryProviderName );
		scheduleTask( updateTask, period );
		this.current = currentToBe;
		started = true;
	}

	@Override
	public Directory getDirectory() {
		if ( !started ) {
			if ( dummyDirectory == null ) {
				RAMDirectory directory = new RAMDirectory();
				DirectoryHelper.initializeIndexIfNeeded( directory );
				dummyDirectory = directory;
			}
			return dummyDirectory;
		}

		int readState = current;// to have the read consistent in the next two "if"s.
		if ( readState == 1 ) {
			return directory1;
		}
		else if ( readState == 2 ) {
			return directory2;
		}
		else {
			throw new AssertionFailure( "Illegal current directory: " + readState );
		}
	}

	@Override
	public boolean equals(Object obj) {
		throw new AssertionFailure( "this type can not be compared reliably" );
	}

	@Override
	public int hashCode() {
		throw new AssertionFailure( "this type can not be compared reliably" );
	}

	class InitTask extends TimerTask {

		@Override
		public void run() {
			try {
				if ( attemptInitializeAndStart() ) {
					// then this task is no longer needed
					cancel();
				}
			}
			catch (RuntimeException re) {
				// we need this to make sure the error is logged somewhere,
				// as we're executing it in the timer thread
				log.failedSlaveDirectoryProviderInitialization( indexDir.toString(), re );
			}
		}
	}

	/**
	 * @return true if both initialize and start succeeded
	 */
	protected synchronized boolean attemptInitializeAndStart() {
		if ( !initialized ) {
			if ( currentMarkerIsInSource() ) {
				initialized = true;
				log.foundCurrentMarker();
			}
			else {
				log.noCurrentMarkerInSourceDirectory();
			}
		}
		if ( initialized ) {
			startIt();
		}
		return this.started;
	}

	class UpdateTask extends TimerTask {

		private final ExecutorService executor;
		private final CopyDirectory copyTask;

		public UpdateTask(Path sourceIndexDir, Path indexDir) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new CopyDirectory( sourceIndexDir, indexDir );
		}

		@Override
		public void run() {
			if ( copyTask.inProgress.compareAndSet( false, true ) ) {
				executor.execute( copyTask );
			}
			else {
				if ( log.isTraceEnabled() ) {
					@SuppressWarnings("unused")
					int unneeded = current;//ensure visibility of indexName in Timer threads.
					log.tracef( "Skipping directory synchronization, previous work still in progress: %s", indexDir );
				}
			}
		}

		public void stop() {
			executor.shutdownNow();
		}
	}

	class CopyDirectory implements Runnable {
		private final Path source;
		private final Path destination;
		private final AtomicBoolean inProgress = new AtomicBoolean( false );

		public CopyDirectory(Path sourceIndexDir, Path destination) {
			this.source = sourceIndexDir;
			this.destination = destination;
		}

		@Override
		public void run() {
			long start = System.nanoTime();
			try {
				Path sourceFile = determineCurrentSourceFile();
				if ( sourceFile == null ) {
					log.unableToDetermineCurrentInSourceDirectory();
					return;
				}

				// check whether a copy is needed at all
				Path currentDestinationFile = destination.resolve( Integer.valueOf( current ).toString() );
				try {
					if ( FileHelper.areInSync( sourceFile, currentDestinationFile ) ) {
						log.trace( "Source and destination directory are in sync. No copying required." );
						return;
					}
				}
				catch (IOException ioe) {
					log.unableToCompareSourceWithDestinationDirectory( sourceFile.toString(), currentDestinationFile.toString() );
				}

				// copy is required
				int oldIndex = current;
				int index = oldIndex == 1 ? 2 : 1;
				Path destinationFile = destination.resolve( Integer.valueOf( index ).toString() );
				try {
					log.tracef( "Copying %s into %s", sourceFile, destinationFile );
					FileHelper.synchronize( sourceFile, destinationFile, true );
					current = index;
					log.tracef( "Copy for %s took %d ms", indexDir, TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) );
				}
				catch (IOException e) {
					//don't change current
					log.unableToSynchronizeSource( indexDir.toString(), e );
					return;
				}
				try {
					Files.delete( indexDir.resolve( "current" + oldIndex ) );
				}
				catch (IOException e) {
					log.unableToRemovePreviousMarker( indexDir.toString(), e );
				}
				try {
					Files.createFile( indexDir.resolve( "current" + index ) );
				}
				catch (IOException e) {
					log.unableToCreateCurrentMarker( indexDir.toString(), e );
				}
			}
			finally {
				inProgress.set( false );
			}
		}

		/**
		 * @return Return a file to the currently active source directory. Tests for the files "current1" and
		 *         "current2" in order to determine which is the current directory. If the marker file does not exist
		 *         <code>null</code> is returned.
		 */
		private Path determineCurrentSourceFile() {
			Path sourceFile = null;
			if ( Files.exists( source.resolve( "current1" ) ) ) {
				sourceFile = source.resolve( "1" );
			}
			else if ( Files.exists( source.resolve( "current2" ) ) ) {
				sourceFile = source.resolve( "2" );
			}
			return sourceFile;
		}
	}

	@Override
	public void stop() {
		@SuppressWarnings("unused")
		int readCurrentState = current; //unneeded value, but ensure visibility of state protected by memory barrier
		timer.cancel();
		if ( updateTask != null ) {
			updateTask.stop();
		}
		closeDirectory( directory1 );
		closeDirectory( directory2 );
	}

	private void closeDirectory(Directory directory) {
		if ( directory != null ) {
			try {
				directory.close();
			}
			catch (Exception e) {
				log.unableToCloseLuceneDirectory( directory, e );
			}
		}
	}

	protected void scheduleTask(TimerTask task, long period) {
		timer.schedule( task, period, period );
	}

}
