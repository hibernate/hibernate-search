/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.store.spi.DirectoryHelper;
import org.hibernate.search.store.spi.LockFactoryCreator;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public final class DirectoryProviderHelper {
	private static final Log log = LoggerFactory.make();

	private static final String ROOT_INDEX_PROP_NAME = "sourceBase";
	private static final String RELATIVE_INDEX_PROP_NAME = "source";
	private static final String COPY_BUFFER_SIZE_PROP_NAME = "buffer_size_on_copy";
	private static final String FS_DIRECTORY_TYPE_PROP_NAME = "filesystem_access_type";
	private static final String REFRESH_PROP_NAME = "refresh";
	private static final String RETRY_INITIALIZE_PROP_NAME = "retry_initialize_period";

	private DirectoryProviderHelper() {
	}

	/**
	 * Build a directory name out of a root and relative path, guessing the significant part
	 * and checking for the file availability
	 *
	 * @param indexName the name of the index (directory) to create
	 * @param properties the configuration properties
	 * @param needWritePermissions when true the directory will be tested for read-write permissions.
	 * @return The file representing the source directory
	 */
	public static File getSourceDirectory(String indexName, Properties properties, boolean needWritePermissions) {
		String root = properties.getProperty( ROOT_INDEX_PROP_NAME );
		String relative = properties.getProperty( RELATIVE_INDEX_PROP_NAME );
		File sourceDirectory;
		if ( log.isTraceEnabled() ) {
			log.trace(
					"Guess source directory from " + ROOT_INDEX_PROP_NAME + " " +
							( root != null ? root : "<null>" ) +
							" and " +
							RELATIVE_INDEX_PROP_NAME + " " +
							( relative != null ? relative : "<null>" )
					);
		}
		if ( relative == null ) {
			relative = indexName;
		}
		if ( StringHelper.isEmpty( root ) ) {
			log.debug( "No root directory, go with relative " + relative );
			sourceDirectory = new File( relative );
			if ( !sourceDirectory.isDirectory() ) { // this also tests for existence
				throw new SearchException( "Unable to read source directory: " + relative );
			}
			//else keep source as it
		}
		else {
			File rootDir = new File( root );
			makeSanityCheckedDirectory( rootDir, indexName, needWritePermissions );
			sourceDirectory = new File( root, relative );
			makeSanityCheckedDirectory( sourceDirectory, indexName, needWritePermissions );
			log.debug( "Got directory from root + relative" );
		}
		return sourceDirectory;
	}

	/**
	 * Creates an FSDirectory in provided directory and initializes
	 * an index if not already existing.
	 *
	 * @param indexDir the directory where to write a new index
	 * @param properties the configuration properties
	 * @return the created {@code FSDirectory} instance
	 * @throws java.io.IOException if an error
	 */
	public static FSDirectory createFSIndex(File indexDir, Properties properties, ServiceManager serviceManager) throws IOException {
		LockFactory lockFactory = getLockFactory( indexDir, properties, serviceManager );
		FSDirectoryType fsDirectoryType = FSDirectoryType.getType( properties );
		FSDirectory fsDirectory = fsDirectoryType.getDirectory( indexDir, null );

		// must use the setter (instead of using the constructor) to set the lockFactory, or Lucene will
		// throw an exception if it's different than a previous setting.
		fsDirectory.setLockFactory( lockFactory );
		log.debugf( "Initialize index: '%s'", indexDir.getAbsolutePath() );
		DirectoryHelper.initializeIndexIfNeeded( fsDirectory );
		return fsDirectory;
	}

	private static LockFactory getLockFactory(File indexDir, Properties properties, ServiceManager serviceManager) {
		try {
			return serviceManager.requestService( LockFactoryCreator.class ).createLockFactory( indexDir, properties );
		}
		finally {
			serviceManager.releaseService( LockFactoryCreator.class );
		}
	}

	/**
	 * @param directory The directory to create or verify
	 * @param indexName To label exceptions
	 * @param verifyIsWritable Verify the directory is writable
	 *
	 * @throws SearchException
	 */
	public static void makeSanityCheckedDirectory(File directory, String indexName, boolean verifyIsWritable) {
		if ( !directory.exists() ) {
			log.indexDirectoryNotFoundCreatingNewOne( directory.getAbsolutePath() );
			//if not existing, create the full path
			if ( !directory.mkdirs() ) {
				throw new SearchException(
						"Unable to create index directory: "
								+ directory.getAbsolutePath() + " for index "
								+ indexName
				);
			}
		}
		else {
			// else check it is not a file
			if ( !directory.isDirectory() ) {
				throw new SearchException(
						"Unable to initialize index: "
								+ indexName + ": "
								+ directory.getAbsolutePath() + " is a file."
				);
			}
		}
		// and ensure it's writable
		if ( verifyIsWritable && ( !directory.canWrite() ) ) {
			throw new SearchException(
					"Cannot write into index directory: "
							+ directory.getAbsolutePath() + " for index "
							+ indexName
			);
		}
	}

	/**
	 * @param properties the configuration of the DirectoryProvider
	 * @param directoryProviderName the name of the DirectoryProvider, used for error reporting
	 * @return The period in milliseconds to keep retrying initialization of a DirectoryProvider
	 */
	static long getRetryInitializePeriod(Properties properties, String directoryProviderName) {
		int retry_period_seconds = ConfigurationParseHelper.getIntValue( properties, RETRY_INITIALIZE_PROP_NAME, 0 );
		log.debugf( "Retry initialize period for Directory %s: %d seconds", directoryProviderName, retry_period_seconds );
		if ( retry_period_seconds < 0 ) {
			throw new SearchException( RETRY_INITIALIZE_PROP_NAME + " for Directory " + directoryProviderName + " must be a positive integer" );
		}
		return retry_period_seconds * 1000; //convert into milliseconds
	}

	static long getRefreshPeriod(Properties properties, String directoryProviderName) {
		String refreshPeriod = properties.getProperty( REFRESH_PROP_NAME, "3600" );
		long period;
		try {
			period = Long.parseLong( refreshPeriod );
		}
		catch (NumberFormatException nfe) {
			throw new SearchException(
					"Unable to initialize index: " + directoryProviderName + "; refresh period is not numeric.", nfe
			);
		}
		log.debugf( "Refresh period: %d seconds", period );
		return period * 1000; //per second
	}

	/**
	 * Users may configure the number of MB to use as
	 * "chunk size" for large file copy operations performed
	 * by DirectoryProviders.
	 *
	 * @param indexName the index name
	 * @param properties the configuration properties
	 * @return the number of Bytes to use as "chunk size" in file copy operations.
	 */
	public static long getCopyBufferSize(String indexName, Properties properties) {
		String value = properties.getProperty( COPY_BUFFER_SIZE_PROP_NAME );
		long size = FileHelper.DEFAULT_COPY_BUFFER_SIZE;
		if ( value != null ) {
			try {
				size = Long.parseLong( value ) * 1024 * 1024; //from MB to B.
			}
			catch (NumberFormatException nfe) {
				throw new SearchException(
						"Unable to initialize index " +
								indexName + "; " + COPY_BUFFER_SIZE_PROP_NAME + " is not numeric.", nfe
				);
			}
			if ( size <= 0 ) {
				throw new SearchException(
						"Unable to initialize index " +
								indexName + "; " + COPY_BUFFER_SIZE_PROP_NAME + " needs to be greater than zero."
				);
			}
		}
		return size;
	}

	private enum FSDirectoryType {
		AUTO( null ),
		SIMPLE( SimpleFSDirectory.class ),
		NIO( NIOFSDirectory.class ),
		MMAP( MMapDirectory.class );

		private Class<?> fsDirectoryClass;

		FSDirectoryType(Class<?> fsDirectoryClass) {
			this.fsDirectoryClass = fsDirectoryClass;
		}

		public FSDirectory getDirectory(File indexDir, LockFactory factory) throws IOException {
			FSDirectory directory;
			if ( fsDirectoryClass == null ) {
				directory = FSDirectory.open( indexDir, factory );
			}
			else {
				try {
					Constructor constructor = fsDirectoryClass.getConstructor( File.class, LockFactory.class );
					directory = (FSDirectory) constructor.newInstance( indexDir, factory );
				}
				catch (NoSuchMethodException e) {
					throw new SearchException( "Unable to find appropriate FSDirectory constructor", e );
				}
				catch (InstantiationException e) {
					throw new SearchException(
							"Unable to instantiate FSDirectory class " + fsDirectoryClass.getName(), e
					);
				}
				catch (IllegalAccessException e) {
					throw new SearchException(
							"Unable to instantiate FSDirectory class " + fsDirectoryClass.getName(), e
					);
				}
				catch (InvocationTargetException e) {
					throw new SearchException(
							"Unable to instantiate FSDirectory class " + fsDirectoryClass.getName(), e
					);
				}
			}
			return directory;
		}

		public static FSDirectoryType getType(Properties properties) {
			FSDirectoryType fsDirectoryType;
			String fsDirectoryTypeValue = properties.getProperty( FS_DIRECTORY_TYPE_PROP_NAME );
			if ( StringHelper.isNotEmpty( fsDirectoryTypeValue ) ) {
				try {
					fsDirectoryType = Enum.valueOf( FSDirectoryType.class, fsDirectoryTypeValue.toUpperCase() );
				}
				catch (IllegalArgumentException e) {
					throw new SearchException( "Invalid option value for " + FS_DIRECTORY_TYPE_PROP_NAME + ": " + fsDirectoryTypeValue );
				}
			}
			else {
				fsDirectoryType = AUTO;
			}
			return fsDirectoryType;
		}
	}
}
