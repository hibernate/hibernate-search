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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
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
	 * @deprecated Use getSourceDirectoryPath
	 * @param indexName the name of the index (directory) to create
	 * @param properties the configuration properties
	 * @param needWritePermissions when true the directory will be tested for read-write permissions.
	 * @return The file representing the source directory
	 */
	@Deprecated
	public static File getSourceDirectory(String indexName, Properties properties, boolean needWritePermissions) {
		return getSourceDirectoryPath( indexName, properties, needWritePermissions ).toFile();
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
	public static Path getSourceDirectoryPath(String indexName, Properties properties, boolean needWritePermissions) {
		String root = properties.getProperty( ROOT_INDEX_PROP_NAME );
		String relative = properties.getProperty( RELATIVE_INDEX_PROP_NAME );
		Path sourceDirectory;
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
			sourceDirectory = FileSystems.getDefault().getPath( relative );
			if ( Files.notExists( sourceDirectory ) ) {
				throw log.sourceDirectoryNotExisting( relative );
			}
			else if ( ! Files.isReadable( sourceDirectory ) ) {
				throw log.directoryIsNotReadable( relative );
			}
			else if ( ! Files.isDirectory( sourceDirectory ) ) {
				throw log.fileIsADirectory( relative );
			}
			//else keep source as it
		}
		else {
			Path rootDir = FileSystems.getDefault().getPath( root );
			makeSanityCheckedDirectory( rootDir, indexName, needWritePermissions );
			sourceDirectory = rootDir.resolve( relative );
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
	 * @param serviceManager provides access to services
	 * @return the created {@code FSDirectory} instance
	 * @throws java.io.IOException if an error
	 */
	public static FSDirectory createFSIndex(File indexDir, Properties properties, ServiceManager serviceManager) throws IOException {
		LockFactory lockFactory = getLockFactory( indexDir, properties, serviceManager );
		FSDirectoryType fsDirectoryType = FSDirectoryType.getType( properties );
		FSDirectory fsDirectory = fsDirectoryType.getDirectory( indexDir.toPath(), lockFactory );
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
	 * @throws SearchException if the index cannot be created, it's not a directory or it's not writeable
	 */
	public static void makeSanityCheckedDirectory(Path directory, String indexName, boolean verifyIsWritable) {
		if ( Files.notExists( directory ) ) {
			log.indexDirectoryNotFoundCreatingNewOne( directory.toString() );
			//if not existing, create the full path
			try {
				Files.createDirectories( directory );
			}
			catch (IOException e) {
				throw new SearchException(
						"Unable to create index directory: "
								+ directory + " for index " + indexName );
			}
		}
		else {
			// else check it is not a file
			if ( ! Files.isDirectory( directory ) ) {
				throw new SearchException(
						"Unable to initialize index: "
								+ indexName + ": " + directory + " is a file." );
			}
		}
		// and ensure it's writable
		if ( verifyIsWritable && ( ! Files.isWritable( directory ) ) ) {
			throw new SearchException(
					"Cannot write into index directory: "
							+ directory + " for index " + indexName );
		}
	}

	/**
	 * @deprecated Use makeSanityCheckedDirectory(Path directory, String indexName, boolean verifyIsWritable)
	 *
	 * @param directory The directory to create or verify
	 * @param indexName To label exceptions
	 * @param verifyIsWritable Verify the directory is writable
	 */
	@Deprecated
	public static void makeSanityCheckedDirectory(File directory, String indexName, boolean verifyIsWritable) {
		makeSanityCheckedDirectory( directory.toPath(), indexName, verifyIsWritable );
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

		private Class<? extends FSDirectory> fsDirectoryClass;

		FSDirectoryType(Class<? extends FSDirectory> fsDirectoryClass) {
			this.fsDirectoryClass = fsDirectoryClass;
		}

		public FSDirectory getDirectory(Path indexDir, LockFactory factory) throws IOException {
			FSDirectory directory;
			if ( fsDirectoryClass == null ) {
				directory = FSDirectory.open( indexDir, factory );
			}
			else {
				try {
					Constructor<? extends FSDirectory> constructor = fsDirectoryClass.getConstructor( Path.class, LockFactory.class );
					directory = constructor.newInstance( indexDir, factory );
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
					fsDirectoryType = Enum.valueOf( FSDirectoryType.class, fsDirectoryTypeValue.toUpperCase( Locale.ROOT ) );
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
