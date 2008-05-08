//$Id$
package org.hibernate.search.store;

import java.util.Properties;
import java.io.File;
import java.io.IOException;

import org.hibernate.search.SearchException;
import org.hibernate.annotations.common.util.StringHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class DirectoryProviderHelper {
	
	private static final Log log = LogFactory.getLog( DirectoryProviderHelper.class );
	private static final String ROOTINDEX_PROP_NAME = "sourceBase";
	private static final String RELATIVEINDEX_PROP_NAME = "source";

	/**
	 * Build a directory name out of a root and relative path, guessing the significant part
	 * and checking for the file availability
	 * @param directoryProviderName
	 * @param properties
	 * @param needWritePermissions when true the directory will be tested for read-write permissions. 
	 * @return
	 */
	public static File getSourceDirectory( String directoryProviderName, Properties properties, boolean needWritePermissions ) {
		String root = properties.getProperty( ROOTINDEX_PROP_NAME );
		String relative = properties.getProperty( RELATIVEINDEX_PROP_NAME );
		File sourceDirectory;
		if ( log.isTraceEnabled() ) {
			log.trace(
					"Guess source directory from " + ROOTINDEX_PROP_NAME + " " + ( root != null ? root : "<null>" )
							+ " and " + RELATIVEINDEX_PROP_NAME + " " + (relative != null ? relative : "<null>")
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
			log.debug( "Get directory from root + relative" );
		}
		return sourceDirectory;
	}
	
	/**
	 * Creates an FSDirectory in provided directory if not already existing.
	 * @param indexDir The directory where to write a new index
	 * @return the created FSDirectory
	 * @throws IOException
	 */
	public static FSDirectory createFSIndex(File indexDir) throws IOException {
		FSDirectory fsDirectory = FSDirectory.getDirectory( indexDir );
		if ( ! IndexReader.indexExists( fsDirectory ) ) {
			log.debug( "Initialize index: '" + indexDir.getAbsolutePath() + "'" );
			IndexWriter iw = new IndexWriter( fsDirectory, new StandardAnalyzer(), true );
			iw.close();
		}
		return fsDirectory;
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
		log.debug( "Refresh period " + period + " seconds" );
		return period * 1000; //per second
	}
	
}
