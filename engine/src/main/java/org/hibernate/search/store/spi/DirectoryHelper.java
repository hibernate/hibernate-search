/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.spi;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.store.impl.DirectoryProviderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides utility functions around Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class DirectoryHelper {

	private static final Log log = LoggerFactory.make();

	private DirectoryHelper() {
	}

	/**
	 * Initialize the Lucene Directory if it isn't already.
	 *
	 * @param directory the Directory to initialize
	 * @throws SearchException in case of lock acquisition timeouts, IOException, or if a corrupt index is found
	 */
	public static void initializeIndexIfNeeded(Directory directory) {
		//version doesn't really matter as we won't use the Analyzer
		Version version = Environment.DEFAULT_LUCENE_MATCH_VERSION;
		SimpleAnalyzer analyzer = new SimpleAnalyzer( version );
		try {
			if ( ! DirectoryReader.indexExists( directory ) ) {
				try {
					IndexWriterConfig iwriterConfig = new IndexWriterConfig( version, analyzer ).setOpenMode( OpenMode.CREATE );
					//Needs to have a timeout higher than zero to prevent race conditions over (network) RPCs
					//for distributed indexes (Infinispan but probably also NFS and similar)
					iwriterConfig.setWriteLockTimeout( 2000 );
					IndexWriter iw = new IndexWriter( directory, iwriterConfig );
					iw.close();
				}
				catch (LockObtainFailedException lofe) {
					log.lockingFailureDuringInitialization( directory.toString() );
				}
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
	 * Verify the index directory exists and is writable,
	 * or creates it if not existing.
	 *
	 * @param annotatedIndexName The index name declared on the @Indexed annotation
	 * @param properties The properties may override the indexname.
	 * @param verifyIsWritable Verify the directory is writable
	 * @return the File representing the Index Directory
	 * @throws SearchException if any.
	 */
	public static File getVerifiedIndexDir(String annotatedIndexName, Properties properties, boolean verifyIsWritable) {
		String indexBase = properties.getProperty( Environment.INDEX_BASE_PROP_NAME, "." );
		String indexName = properties.getProperty( Environment.INDEX_NAME_PROP_NAME, annotatedIndexName );
		File baseIndexDir = new File( indexBase );
		DirectoryProviderHelper.makeSanityCheckedDirectory( baseIndexDir, indexName, verifyIsWritable );
		File indexDir = new File( baseIndexDir, indexName );
		DirectoryProviderHelper.makeSanityCheckedDirectory( indexDir, indexName, verifyIsWritable );
		return indexDir;
	}
}
