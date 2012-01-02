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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Version;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.store.LockFactoryProvider;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
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
	private static final String LOCKING_STRATEGY_PROP_NAME = "locking_strategy";
	private static final String FS_DIRECTORY_TYPE_PROP_NAME = "filesystem_access_type";
	private static final String INDEX_BASE_PROP_NAME = "indexBase";
	private static final String INDEX_NAME_PROP_NAME = "indexName";
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
	 *
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
	 *
	 * @return the created {@code FSDirectory} instance
	 *
	 * @throws IOException if an error
	 */
	public static FSDirectory createFSIndex(File indexDir, Properties properties) throws IOException {
		LockFactory lockFactory = createLockFactory( indexDir, properties );
		FSDirectoryType fsDirectoryType = FSDirectoryType.getType( properties );
		FSDirectory fsDirectory = fsDirectoryType.getDirectory( indexDir, null );

		// must use the setter (instead of using the constructor) to set the lockFactory, or Lucene will
		// throw an exception if it's different than a previous setting.
		fsDirectory.setLockFactory( lockFactory );
		log.debugf( "Initialize index: '%s'", indexDir.getAbsolutePath() );
		initializeIndexIfNeeded( fsDirectory );
		return fsDirectory;
	}

	/**
	 * Initialize the Lucene Directory if it isn't already.
	 * @param directory the Directory to initialize
	 * @throws SearchException in case of lock acquisition timeouts, IOException, or if a corrupt index is found
	 */
	public static void initializeIndexIfNeeded(Directory directory) {
		//version doesn't really matter as we won't use the Analyzer
		Version version =  Environment.DEFAULT_LUCENE_MATCH_VERSION;
		SimpleAnalyzer analyzer = new SimpleAnalyzer( version );
		try {
			if ( ! IndexReader.indexExists( directory ) ) {
				IndexWriterConfig iwriterConfig = new IndexWriterConfig( version, analyzer ).setOpenMode( OpenMode.CREATE ); 
				IndexWriter iw = new IndexWriter( directory, iwriterConfig );
				iw.close();
			}
		}
		catch (IOException e) {
			throw new SearchException( "Could not initialize index", e );
		}
		finally {
			analyzer.close();
		}
	}

	/**
	 * Creates a LockFactory as selected in the configuration for the
	 * DirectoryProvider.
	 * The SimpleFSLockFactory and NativeFSLockFactory need a File to know
	 * where to stock the filesystem based locks; other implementations
	 * ignore this parameter.
	 *
	 * @param indexDir the directory to use to store locks, if needed by implementation
	 * @param dirConfiguration the configuration of current DirectoryProvider
	 *
	 * @return the LockFactory as configured, or a SimpleFSLockFactory
	 *         in case of configuration errors or as a default.
	 *
	 * @throws IOException
	 */
	public static LockFactory createLockFactory(File indexDir, Properties dirConfiguration) {
		//For FS-based indexes default to "native", default to "single" otherwise.
		String defaultStrategy = indexDir == null ? "single" : "native";
		String lockFactoryName = dirConfiguration.getProperty( LOCKING_STRATEGY_PROP_NAME, defaultStrategy );
		if ( "simple".equals( lockFactoryName ) ) {
			if ( indexDir == null ) {
				throw new SearchException( "To use \"simple\" as a LockFactory strategy an indexBase path must be set" );
			}
			try {
				return new SimpleFSLockFactory( indexDir );
			}
			catch ( IOException e ) {
				throw new SearchException( "Could not initialize SimpleFSLockFactory", e );
			}
		}
		else if ( "native".equals( lockFactoryName ) ) {
			if ( indexDir == null ) {
				throw new SearchException( "To use \"native\" as a LockFactory strategy an indexBase path must be set" );
			}
			try {
				return new NativeFSLockFactory( indexDir );
			}
			catch ( IOException e ) {
				throw new SearchException( "Could not initialize NativeFSLockFactory", e );
			}
		}
		else if ( "single".equals( lockFactoryName ) ) {
			return new SingleInstanceLockFactory();
		}
		else if ( "none".equals( lockFactoryName ) ) {
			return NoLockFactory.getNoLockFactory();
		}
		else {
			LockFactoryProvider lockFactoryFactory = ClassLoaderHelper.instanceFromName(
					LockFactoryProvider.class,
					lockFactoryName, DirectoryProviderHelper.class, LOCKING_STRATEGY_PROP_NAME
			);
			return lockFactoryFactory.createLockFactory( indexDir, dirConfiguration );
		}
	}

	/**
	 * Verify the index directory exists and is writable,
	 * or creates it if not existing.
	 *
	 * @param annotatedIndexName The index name declared on the @Indexed annotation
	 * @param properties The properties may override the indexname.
	 * @param verifyIsWritable Verify the directory is writable
	 *
	 * @return the File representing the Index Directory
	 *
	 * @throws SearchException
	 */
	public static File getVerifiedIndexDir(String annotatedIndexName, Properties properties, boolean verifyIsWritable) {
		String indexBase = properties.getProperty( INDEX_BASE_PROP_NAME, "." );
		String indexName = properties.getProperty( INDEX_NAME_PROP_NAME, annotatedIndexName );
		File baseIndexDir = new File( indexBase );
		makeSanityCheckedDirectory( baseIndexDir, indexName, verifyIsWritable );
		File indexDir = new File( baseIndexDir, indexName );
		makeSanityCheckedDirectory( indexDir, indexName, verifyIsWritable );
		return indexDir;
	}

	/**
	 * @param directory The directory to create or verify
	 * @param indexName To label exceptions
	 * @param verifyIsWritable Verify the directory is writable
	 *
	 * @throws SearchException
	 */
	private static void makeSanityCheckedDirectory(File directory, String indexName, boolean verifyIsWritable) {
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
		catch ( NumberFormatException nfe ) {
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
	 *
	 * @return the number of Bytes to use as "chunk size" in file copy operations.
	 */
	public static long getCopyBufferSize(String indexName, Properties properties) {
		String value = properties.getProperty( COPY_BUFFER_SIZE_PROP_NAME );
		long size = FileHelper.DEFAULT_COPY_BUFFER_SIZE;
		if ( value != null ) {
			try {
				size = Long.parseLong( value ) * 1024 * 1024; //from MB to B.
			}
			catch ( NumberFormatException nfe ) {
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
				catch ( NoSuchMethodException e ) {
					throw new SearchException( "Unable to find appropriate FSDirectory constructor", e );
				}
				catch ( InstantiationException e ) {
					throw new SearchException(
							"Unable to instantiate FSDirectory class " + fsDirectoryClass.getName(), e
					);
				}
				catch ( IllegalAccessException e ) {
					throw new SearchException(
							"Unable to instantiate FSDirectory class " + fsDirectoryClass.getName(), e
					);
				}
				catch ( InvocationTargetException e ) {
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
				catch ( IllegalArgumentException e ) {
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
