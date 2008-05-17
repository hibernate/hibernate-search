//$Id$
package org.hibernate.search.store;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.FSDirectory;
import org.hibernate.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File based directory provider that takes care of getting a version of the index
 * from a given source
 * The base directory is represented by hibernate.search.<index>.indexBase
 * The index is created in <base directory>/<index name>
 * The source (aka copy) directory is built from <sourceBase>/<index name>
 *
 * A copy is triggered every refresh seconds
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class FSSlaveDirectoryProvider implements DirectoryProvider<FSDirectory> {
	
	private final Logger log = LoggerFactory.getLogger( FSSlaveDirectoryProvider.class );
	
	private FSDirectory directory1;
	private FSDirectory directory2;
	private int current;
	private String indexName;
	private Timer timer;

	//variables needed between initialize and start
	private File sourceIndexDir;
	private File indexDir;
	private String directoryProviderName;
	private Properties properties;

	public void initialize(String directoryProviderName, Properties properties, SearchFactoryImplementor searchFactoryImplementor) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		//source guessing
		sourceIndexDir = DirectoryProviderHelper.getSourceDirectory( directoryProviderName, properties, false );
		if ( ! new File( sourceIndexDir, "current1" ).exists() && ! new File( sourceIndexDir, "current2" ).exists() ) {
			throw new IllegalStateException( "No current marker in source directory" );
		}
		log.debug( "Source directory: {}", sourceIndexDir.getPath() );
		indexDir = DirectoryProviderHelper.getVerifiedIndexDir( directoryProviderName, properties, true );
		log.debug( "Index directory: {}", indexDir.getPath() );
		try {
			indexName = indexDir.getCanonicalPath();
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
	}

	public void start() {
		long period = DirectoryProviderHelper.getRefreshPeriod( properties, directoryProviderName );
		try {
			directory1 = DirectoryProviderHelper.createFSIndex( new File(indexDir, "1") );
			directory2 = DirectoryProviderHelper.createFSIndex( new File(indexDir, "2") );
			File currentMarker = new File( indexDir, "current1" );
			File current2Marker = new File( indexDir, "current2" );
			if ( currentMarker.exists() ) {
				current = 1;
				if ( current2Marker.exists() ) {
					current2Marker.delete(); //TODO or throw an exception?
				}
			}
			else if ( current2Marker.exists() ) {
				current = 2;
			}
			else {
				//no default
				log.debug( "Setting directory 1 as current");
				current = 1;
				File destinationFile = new File( indexDir, Integer.valueOf( current ).toString() );
				int sourceCurrent;
				if ( new File( sourceIndexDir, "current1").exists() ) {
					sourceCurrent = 1;
				}
				else if ( new File( sourceIndexDir, "current2").exists() ) {
					sourceCurrent = 2;
				}
				else {
					throw new AssertionFailure( "No current file marker found in source directory: " + sourceIndexDir.getPath() );
				}
				try {
					FileHelper.synchronize( new File( sourceIndexDir, String.valueOf(sourceCurrent) ), destinationFile, true);
				}
				catch (IOException e) {
					throw new SearchException( "Unable to synchronize directory: " + indexName, e );
				}
				if (! currentMarker.createNewFile() ) {
					throw new SearchException( "Unable to create the directory marker file: " + indexName );
				}
			}
			log.debug( "Current directory: {}", current);
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		timer = new Timer(true); //daemon thread, the copy algorithm is robust
		TimerTask task = new TriggerTask( sourceIndexDir, indexDir );
		timer.scheduleAtFixedRate( task, period, period );
	}

	//FIXME this is Thread-Unsafe! A memory barrier is missing.
	public FSDirectory getDirectory() {
		if (current == 1) {
			return directory1;
		}
		else if (current == 2) {
			return directory2;
		}
		else {
			throw new AssertionFailure( "Illegal current directory: " + current );
		}
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) return true;
		if ( obj == null || !( obj instanceof FSSlaveDirectoryProvider ) ) return false;
		return indexName.equals( ( (FSSlaveDirectoryProvider) obj ).indexName );
	}

	@Override
	public int hashCode() {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		int hash = 11;
		return 37 * hash + indexName.hashCode();
	}

	class TriggerTask extends TimerTask {

		private final ExecutorService executor;
		private final CopyDirectory copyTask;

		public TriggerTask(File sourceIndexDir, File destination) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new CopyDirectory( sourceIndexDir, destination  );
		}

		public void run() {
			if (!copyTask.inProgress) {
				executor.execute( copyTask );
			}
			else {
				log.trace( "Skipping directory synchronization, previous work still in progress: {}", indexName);
			}
		}
	}

	class CopyDirectory implements Runnable {
		private final File source;
		private final File destination;
		private volatile boolean inProgress;

		public CopyDirectory(File sourceIndexDir, File destination) {
			this.source = sourceIndexDir;
			this.destination = destination;
		}

		public void run() {
			long start = System.currentTimeMillis();
			try {
				inProgress = true;
				int oldIndex = current;
				int index = current == 1 ? 2 : 1;
				File sourceFile;
				if ( new File( source, "current1" ).exists() ) {
					sourceFile = new File(source, "1");
				}
				else if ( new File( source, "current2" ).exists() ) {
					sourceFile = new File(source, "2");
				}
				else {
					log.error( "Unable to determine current in source directory" );
					inProgress = false;
					return;
				}

				File destinationFile = new File( destination, Integer.valueOf( index ).toString() );
				//TODO make smart a parameter
				try {
					log.trace( "Copying {} into {}", sourceFile, destinationFile );
					FileHelper.synchronize( sourceFile, destinationFile, true );
					current = index;
				}
				catch (IOException e) {
					//don't change current
					log.error( "Unable to synchronize " + indexName, e);
					inProgress = false;
					return;
				}
				if ( ! new File( indexName, "current" + oldIndex ).delete() ) {
					log.warn( "Unable to remove previous marker file in " + indexName );
				}
				try {
					new File( indexName, "current" + index ).createNewFile();
				}
				catch( IOException e ) {
					log.warn( "Unable to create current marker file in " + indexName, e );
				}
			}
			finally {
				inProgress = false;
			}
			log.trace( "Copy for {} took {} ms", indexName, (System.currentTimeMillis() - start) );
		}
	}

	public void finalize() throws Throwable {
		super.finalize();
		timer.cancel();
		//TODO find a better cycle from Hibernate core
	}
}
