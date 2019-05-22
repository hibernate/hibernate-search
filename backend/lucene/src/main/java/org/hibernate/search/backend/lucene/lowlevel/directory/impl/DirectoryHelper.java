/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerConstants;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SleepingLockWrapper;

/**
 * Provides utility functions around Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
class DirectoryHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private DirectoryHelper() {
	}

	/**
	 * Initialize the Lucene Directory if it isn't already.
	 *
	 * @param directory the Directory to initialize
	 * @throws SearchException in case of lock acquisition timeouts, IOException, or if a corrupt index is found
	 */
	public static void initializeIndexIfNeeded(Directory directory, EventContext eventContext) throws IOException {
		if ( DirectoryReader.indexExists( directory ) ) {
			return;
		}

		try {
			IndexWriterConfig iwriterConfig = new IndexWriterConfig( AnalyzerConstants.KEYWORD_ANALYZER )
					.setOpenMode( OpenMode.CREATE_OR_APPEND );
			//Needs to have a timeout higher than zero to prevent race conditions over (network) RPCs
			//for distributed indexes (Infinispan but probably also NFS and similar)
			SleepingLockWrapper delayedDirectory = new SleepingLockWrapper( directory, 2000, 20 );
			IndexWriter iw = new IndexWriter( delayedDirectory, iwriterConfig );
			iw.close();
		}
		catch (LockObtainFailedException lofe) {
			log.lockingFailureDuringInitialization( directory.toString(), eventContext );
		}
	}

	public static void makeSanityCheckedFilesystemDirectory(Path indexDirectory, EventContext eventContext) {
		if ( Files.exists( indexDirectory ) ) {
			if ( !Files.isDirectory( indexDirectory ) || !Files.isWritable( indexDirectory ) ) {
				throw log.localDirectoryIndexRootDirectoryNotWritableDirectory( indexDirectory, eventContext );
			}
		}
		else {
			try {
				Files.createDirectories( indexDirectory );
			}
			catch (Exception e) {
				throw log.unableToCreateIndexRootDirectoryForLocalDirectoryBackend( indexDirectory, eventContext, e );
			}
		}
	}

}
