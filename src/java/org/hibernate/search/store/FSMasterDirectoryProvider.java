//$Id$
package org.hibernate.search.store;

import java.util.Timer;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.search.util.DirectoryProviderHelper;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.HibernateException;

/**
 * File based DirectoryProvider that takes care of index copy
 * The base directory is represented by hibernate.search.<index>.indexBase
 * The index is created in <base directory>/<index name>
 * The source (aka copy) directory is built from <sourceBase>/<index name>
 *
 * A copy is triggered every refresh seconds
 *
 * @author Emmanuel Bernard
 */
//TODO rename copy?
public class FSMasterDirectoryProvider implements DirectoryProvider<FSDirectory> {
	private static Log log = LogFactory.getLog( FSMasterDirectoryProvider.class );
	private FSDirectory directory;
	private int current;
	private String indexName;
	private Timer timer;
	private SearchFactoryImplementor searchFactory;

	//variables needed between initialize and start
	private String source;
	private File indexDir;
	private String directoryProviderName;
	private Properties properties;


	public void initialize(String directoryProviderName, Properties properties, SearchFactoryImplementor searchFactoryImplementor) {
		this.properties = properties;
		this.directoryProviderName = directoryProviderName;
		//source guessing
		source = DirectoryProviderHelper.getSourceDirectory( "sourceBase", "source", directoryProviderName, properties );
		if ( source == null)
			throw new IllegalStateException("FSMasterDirectoryProvider requires a viable source directory");
		log.debug( "Source directory: " + source );
		indexDir = DirectoryProviderHelper.determineIndexDir( directoryProviderName, properties );
		log.debug( "Index directory: " + indexDir );
		try {
			boolean create = !indexDir.exists();
			if (create) {
				log.debug( "Index directory '" + indexName + "' will be initialized");
				indexDir.mkdirs();
			}
			indexName = indexDir.getCanonicalPath();
			directory = FSDirectory.getDirectory( indexName);
			if ( create ) {
				IndexWriter iw = new IndexWriter( directory, new StandardAnalyzer(), create );
				iw.close();
			}
		}
		catch (IOException e) {
			throw new HibernateException( "Unable to initialize index: " + directoryProviderName, e );
		}
		this.searchFactory = searchFactoryImplementor;
	}

	public void start() {
		//source guessing
		String refreshPeriod = properties.getProperty( "refresh", "3600" );
		long period = Long.parseLong( refreshPeriod );
		log.debug("Refresh period " + period + " seconds");
		period *= 1000; //per second
		try {
			//copy to source
			if ( new File( source, "current1").exists() ) {
				current = 2;
			}
			else if ( new File( source, "current2").exists() ) {
				current = 1;
			}
			else {
				log.debug( "Source directory for '" + indexName + "' will be initialized");
				current = 1;
			}
			String currentString = Integer.valueOf( current ).toString();
			File subDir = new File( source, currentString );
			FileHelper.synchronize( indexDir, subDir, true );
			new File( source, "current1").delete();
			new File( source, "current2").delete();
			//TODO small hole, no file can be found here
			new File( source, "current" + currentString).createNewFile();
			log.debug( "Current directory: " + current);
		}
		catch (IOException e) {
			throw new HibernateException( "Unable to initialize index: " + directoryProviderName, e );
		}
		timer = new Timer(true); //daemon thread, the copy algorithm is robust
		TimerTask task = new FSMasterDirectoryProvider.TriggerTask(indexName, source, this );
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

	class TriggerTask extends TimerTask {

		private ExecutorService executor;
		private FSMasterDirectoryProvider.CopyDirectory copyTask;

		public TriggerTask(String source, String destination, DirectoryProvider directoryProvider) {
			executor = Executors.newSingleThreadExecutor();
			copyTask = new FSMasterDirectoryProvider.CopyDirectory( source, destination, directoryProvider );
		}

		public void run() {
			if (!copyTask.inProgress) {
				executor.execute( copyTask );
			}
			else {
				log.info( "Skipping directory synchronization, previous work still in progress: " + indexName);
			}
		}
	}

	class CopyDirectory implements Runnable {
		private String source;
		private String destination;
		private volatile boolean inProgress;
		private Lock directoryProviderLock;
		private DirectoryProvider directoryProvider;

		public CopyDirectory(String source, String destination, DirectoryProvider directoryProvider) {
			this.source = source;
			this.destination = destination;
			this.directoryProvider = directoryProvider;
		}

		public void run() {
			//TODO get rid of current and use the marker file instead?
			long start = System.currentTimeMillis();
			inProgress = true;
			if (directoryProviderLock == null) {
				directoryProviderLock = searchFactory.getLockableDirectoryProviders().get( directoryProvider );
				directoryProvider = null;
				searchFactory = null; //get rid of any useless link (help hot redeployment?)
			}
			try {
				directoryProviderLock.lock();
				int oldIndex = current;
				int index = current == 1 ? 2 : 1;
				File sourceFile = new File(source);

				File destinationFile = new File(destination, Integer.valueOf(index).toString() );
				//TODO make smart a parameter
				try {
					log.trace("Copying " + sourceFile + " into " + destinationFile);
					FileHelper.synchronize( sourceFile, destinationFile, true);
					current = index;
				}
				catch (IOException e) {
					//don't change current
					log.error( "Unable to synchronize source of " + indexName, e);
					inProgress = false;
					return;
				}
				if ( ! new File(destination, "current" + oldIndex).delete() ) {
					log.warn( "Unable to remove previous marker file from source of " + indexName );
				}
				try {
					new File(destination, "current" + index).createNewFile();
				}
				catch( IOException e ) {
					log.warn( "Unable to create current marker in source of " + indexName, e );
				}
			}
			finally {
				directoryProviderLock.unlock();
				inProgress = false;
			}
			log.trace( "Copy for " + indexName + " took " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	public void finalize() throws Throwable {
		super.finalize();
		timer.cancel();
		//TODO find a better cycle from Hibernate core
	}
}
