//$Id$
package org.hibernate.search.store;

import java.util.Timer;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

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
	
	private final Logger log = LoggerFactory.getLogger( FSMasterDirectoryProvider.class );
	
	private FSDirectory directory;
	private int current;
	private String indexName;
	private Timer timer;
	private SearchFactoryImplementor searchFactory;
	private long copyChunkSize;

	//variables needed between initialize and start
	private File sourceDir;
	private File indexDir;
	private String directoryProviderName;
	private Properties properties;

	public void initialize(String directoryProviderName, Properties properties, SearchFactoryImplementor searchFactoryImplementor) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		//source guessing
		sourceDir = DirectoryProviderHelper.getSourceDirectory( directoryProviderName, properties, true );
		log.debug( "Source directory: {}", sourceDir.getPath() );
		indexDir = DirectoryProviderHelper.getVerifiedIndexDir( directoryProviderName, properties, true );
		log.debug( "Index directory: {}", indexDir.getPath() );
		try {
			indexName = indexDir.getCanonicalPath();
			directory = DirectoryProviderHelper.createFSIndex( indexDir );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		copyChunkSize = DirectoryProviderHelper.getCopyBufferSize( directoryProviderName, properties );
		this.searchFactory = searchFactoryImplementor;
	}

	public void start() {
		long period = DirectoryProviderHelper.getRefreshPeriod( properties, directoryProviderName );
		try {
			//copy to source
			if ( new File( sourceDir, "current1").exists() ) {
				current = 2;
			}
			else if ( new File( sourceDir, "current2").exists() ) {
				current = 1;
			}
			else {
				log.debug( "Source directory for '{}' will be initialized", indexName);
				current = 1;
			}
			String currentString = Integer.valueOf( current ).toString();
			File subDir = new File( sourceDir, currentString );
			FileHelper.synchronize( indexDir, subDir, true, copyChunkSize );
			new File( sourceDir, "current1 ").delete();
			new File( sourceDir, "current2" ).delete();
			//TODO small hole, no file can be found here
			new File( sourceDir, "current" + currentString ).createNewFile();
			log.debug( "Current directory: {}", current );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
		timer = new Timer( true ); //daemon thread, the copy algorithm is robust
		TimerTask task = new FSMasterDirectoryProvider.TriggerTask( indexDir, sourceDir, this );
		timer.scheduleAtFixedRate( task, period, period );
	}

	public FSDirectory getDirectory() {
		return directory;
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) return true;
		if ( obj == null || !( obj instanceof FSMasterDirectoryProvider ) ) return false;
		return indexName.equals( ( (FSMasterDirectoryProvider) obj ).indexName );
	}

	@Override
	public int hashCode() {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		int hash = 11;
		return 37 * hash + indexName.hashCode();
	}



	public void stop() {
		timer.cancel();
		try {
			directory.close();
		}
		catch (Exception e) {
			log.error( "Unable to properly close Lucene directory {}" + directory.getFile(), e );
		}
	}

	class TriggerTask extends TimerTask {

		private final Executor executor;
		private final FSMasterDirectoryProvider.CopyDirectory copyTask;

		public TriggerTask(File source, File destination, DirectoryProvider directoryProvider) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new FSMasterDirectoryProvider.CopyDirectory( source, destination, directoryProvider );
		}

		public void run() {
			if ( ! copyTask.inProgress ) {
				executor.execute( copyTask );
			}
			else {
				log.info( "Skipping directory synchronization, previous work still in progress: {}", indexName );
			}
		}
	}

	class CopyDirectory implements Runnable {
		private final File source;
		private final File destination;
		private volatile boolean inProgress;
		private Lock directoryProviderLock;
		private DirectoryProvider directoryProvider;

		public CopyDirectory(File source, File destination, DirectoryProvider directoryProvider) {
			this.source = source;
			this.destination = destination;
			this.directoryProvider = directoryProvider;
		}

		public void run() {
			//TODO get rid of current and use the marker file instead?
			long start = System.currentTimeMillis();
			inProgress = true;
			if ( directoryProviderLock == null ) {
				directoryProviderLock = searchFactory.getLockableDirectoryProviders().get( directoryProvider );
				directoryProvider = null;
				searchFactory = null; //get rid of any useless link (help hot redeployment?)
			}
			try {
				directoryProviderLock.lock();
				int oldIndex = current;
				int index = current == 1 ? 2 : 1;

				File destinationFile = new File( destination, Integer.valueOf(index).toString() );
				try {
					log.trace( "Copying {} into {}", source, destinationFile );
					FileHelper.synchronize( source, destinationFile, true, copyChunkSize );
					current = index;
				}
				catch (IOException e) {
					//don't change current
					log.error( "Unable to synchronize source of " + indexName, e );
					inProgress = false;
					return;
				}
				if ( ! new File( destination, "current" + oldIndex ).delete() ) {
					log.warn( "Unable to remove previous marker file from source of {}", indexName );
				}
				try {
					new File( destination, "current" + index ).createNewFile();
				}
				catch( IOException e ) {
					log.warn( "Unable to create current marker in source of " + indexName, e );
				}
			}
			finally {
				directoryProviderLock.unlock();
				inProgress = false;
			}
			log.trace( "Copy for {} took {} ms", indexName, (System.currentTimeMillis() - start) );
		}
	}
}
