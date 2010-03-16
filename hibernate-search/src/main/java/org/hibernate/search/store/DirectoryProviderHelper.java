/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.slf4j.Logger;

import org.hibernate.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.PluginLoader;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class DirectoryProviderHelper {
	
	private static final Logger log = LoggerFactory.make();
	private static final String ROOTINDEX_PROP_NAME = "sourceBase";
	private static final String RELATIVEINDEX_PROP_NAME = "source";
	public static final String COPYBUFFERSIZE_PROP_NAME = "buffer_size_on_copy";

	/**
	 * Build a directory name out of a root and relative path, guessing the significant part
	 * and checking for the file availability
	 * @param directoryProviderName
	 * @param properties
	 * @param needWritePermissions when true the directory will be tested for read-write permissions. 
	 * @return The file representing the source directory
	 */
	public static File getSourceDirectory( String directoryProviderName, Properties properties, boolean needWritePermissions ) {
		String root = properties.getProperty( ROOTINDEX_PROP_NAME );
		String relative = properties.getProperty( RELATIVEINDEX_PROP_NAME );
		File sourceDirectory;
		if ( log.isTraceEnabled() ) {
			log.trace(
					"Guess source directory from {} {} and {} {}", new Object[] {
						ROOTINDEX_PROP_NAME,
						( root != null ? root : "<null>" ),
						RELATIVEINDEX_PROP_NAME,
					(relative != null ? relative : "<null>") }
			);
		}
		if ( relative == null ) relative = directoryProviderName;
		if ( StringHelper.isEmpty( root ) ) {
			log.debug( "No root directory, go with relative " + relative );
			sourceDirectory = new File( relative );
			if ( ! sourceDirectory.isDirectory() ) { // this also tests for existence
				throw new SearchException( "Unable to read source directory: " + relative );
			}
			//else keep source as it
		}
		else {
			File rootDir = new File( root );
			makeSanityCheckedDirectory( rootDir, directoryProviderName, needWritePermissions );
			sourceDirectory = new File( root, relative );
			makeSanityCheckedDirectory( sourceDirectory, directoryProviderName, needWritePermissions );
			log.debug( "Got directory from root + relative" );
		}
		return sourceDirectory;
	}
	
	/**
	 * Creates an FSDirectory in provided directory and initializes
	 * an index if not already existing.
	 * @param indexDir The directory where to write a new index
	 * @return the created FSDirectory
	 * @throws IOException
	 */
	public static FSDirectory createFSIndex(File indexDir, Properties dirConfiguration) throws IOException {
		LockFactory lockFactory = createLockFactory(indexDir, dirConfiguration);
		FSDirectory fsDirectory = FSDirectory.open( indexDir, null );
		// must use the setter (instead of using the constructor) to set the lockFactory, or Lucene will
		// throw an exception if it's different than a previous setting.
		fsDirectory.setLockFactory( lockFactory );
		if ( ! IndexReader.indexExists( fsDirectory ) ) {
			log.debug( "Initialize index: '{}'", indexDir.getAbsolutePath() );
			IndexWriter.MaxFieldLength fieldLength = new IndexWriter.MaxFieldLength( IndexWriter.DEFAULT_MAX_FIELD_LENGTH );
			IndexWriter iw = new IndexWriter( fsDirectory, new SimpleAnalyzer(), true, fieldLength );
			iw.close();
		}
		return fsDirectory;
	}
	
	/**
	 * Creates a LockFactory as selected in the configuration for the
	 * DirectoryProvider.
	 * The SimpleFSLockFactory and NativeFSLockFactory need a File to know
	 * where to stock the filesystem based locks; other implementations
	 * ignore this parameter.
	 * @param indexDir the directory to use to store locks, if needed by implementation
	 * @param dirConfiguration the configuration of current DirectoryProvider
	 * @return the LockFactory as configured, or a SimpleFSLockFactory
	 * in case of configuration errors or as a default.
	 * @throws IOException
	 */
	public static LockFactory createLockFactory(File indexDir, Properties dirConfiguration) {
		//For FS-based indexes default to "simple", default to "single" otherwise.
		String defaultStrategy = indexDir==null ? "single" : "simple";
		String lockFactoryName = dirConfiguration.getProperty( "locking_strategy", defaultStrategy );
		if ( "simple".equals( lockFactoryName ) ) {
			if ( indexDir==null ) {
				throw new SearchException( "To use \"simple\" as a LockFactory strategy an indexBase path must be set");
			}
			try {
				return new SimpleFSLockFactory( indexDir );
			} catch (IOException e) {
				throw new SearchException( "Could not initialize SimpleFSLockFactory", e);
			}
		}
		else if ( "native".equals( lockFactoryName ) ) {
			if ( indexDir==null ) {
				throw new SearchException( "To use \"native\" as a LockFactory strategy an indexBase path must be set");
			}
			try {
				return new NativeFSLockFactory( indexDir );
			} catch (IOException e) {
				throw new SearchException( "Could not initialize NativeFSLockFactory", e);
			}
		}
		else if ( "single".equals( lockFactoryName ) ) {
			return new SingleInstanceLockFactory();
		}
		else if ( "none".equals( lockFactoryName ) ) {
			return new NoLockFactory();
		}
		else {
			LockFactoryFactory lockFactoryFactory = PluginLoader.instanceFromName( LockFactoryFactory.class,
						lockFactoryName, DirectoryProviderHelper.class, "locking_strategy" );
			return lockFactoryFactory.createLockFactory( indexDir, dirConfiguration );
		}
	}

	/**
	 * Verify the index directory exists and is writable,
	 * or creates it if not existing.
	 * @param annotatedIndexName The index name declared on the @Indexed annotation
	 * @param properties The properties may override the indexname.
	 * @param verifyIsWritable Verify the directory is writable
	 * @return the File representing the Index Directory
	 * @throws SearchException
	 */
	public static File getVerifiedIndexDir(String annotatedIndexName, Properties properties, boolean verifyIsWritable) {
		String indexBase = properties.getProperty( "indexBase", "." );
		String indexName = properties.getProperty( "indexName", annotatedIndexName );
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
	 * @throws SearchException
	 */
	private static void makeSanityCheckedDirectory(File directory, String indexName, boolean verifyIsWritable) {
		if ( ! directory.exists() ) {
			log.warn( "Index directory not found, creating: '" + directory.getAbsolutePath() + "'" );
			//if not existing, create the full path
			if ( ! directory.mkdirs() ) {
				throw new SearchException( "Unable to create index directory: "
						+ directory.getAbsolutePath() + " for index "
						+ indexName );
			}
		}
		else {
			// else check it is not a file
			if ( ! directory.isDirectory() ) {
				throw new SearchException( "Unable to initialize index: "
						+ indexName + ": "
						+ directory.getAbsolutePath() + " is a file." );
			}
		}
		// and ensure it's writable
		if ( verifyIsWritable && ( ! directory.canWrite() ) ) {
			throw new SearchException( "Cannot write into index directory: "
					+ directory.getAbsolutePath() + " for index "
					+ indexName );
		}
	}

	static long getRefreshPeriod(Properties properties, String directoryProviderName) {
		String refreshPeriod = properties.getProperty( "refresh", "3600" );
		long period;
		try {
			period = Long.parseLong( refreshPeriod );
		} catch (NumberFormatException nfe) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName +"; refresh period is not numeric.", nfe );
		}
		log.debug( "Refresh period: {} seconds", period );
		return period * 1000; //per second
	}

	/**
	 * Users may configure the number of MB to use as
	 * "chunk size" for large file copy operations performed
	 * by DirectoryProviders.
	 * @param directoryProviderName
	 * @param properties
	 * @return the number of Bytes to use as "chunk size" in file copy operations.
	 */
	public static long getCopyBufferSize(String directoryProviderName, Properties properties) {
		String value = properties.getProperty( COPYBUFFERSIZE_PROP_NAME );
		long size = FileHelper.DEFAULT_COPY_BUFFER_SIZE;
		if ( value != null ) {
			try {
				size = Long.parseLong( value ) * 1024 * 1024; //from MB to B.
			} catch (NumberFormatException nfe) {
				throw new SearchException( "Unable to initialize index " +
						directoryProviderName +"; "+ COPYBUFFERSIZE_PROP_NAME + " is not numeric.", nfe );
			}
			if ( size <= 0 ) {
				throw new SearchException( "Unable to initialize index " +
						directoryProviderName +"; "+ COPYBUFFERSIZE_PROP_NAME + " needs to be greater than zero.");
			}
		}
		return size;
	}
	
}
