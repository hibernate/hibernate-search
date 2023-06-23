/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneTestIndexesPathConfiguration;

import org.junit.AssumptionViolatedException;

import org.jboss.logging.Logger;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LuceneTckBackendAccessor implements TckBackendAccessor {

	private static final Logger log = Logger.getLogger( LuceneTestIndexesPathConfiguration.class.getName() );

	private final Path indexesPath;

	LuceneTckBackendAccessor(Path indexesPath) {
		this.indexesPath = indexesPath;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public void ensureIndexingOperationsFail(String indexName) {
		Path indexPath = indexesPath.resolve( indexName );
		try {
			// Try to corrupt the content of the index
			// Just deleting the content is not enough as Lucene may re-create it,
			// and delete a file currently in use is not possible on Windows anyway.
			Files.list( indexPath ).forEach( path -> deleteOrLogRecursively( path, indexName ) );

			nukeOrLogIndexTopLevelDirectory( indexPath, indexName );
		}
		catch (RuntimeException | IOException e) {
			throw new IllegalStateException(
					"Unexpected exception while deleting and changing files in index '"
							+ indexName + "' at path '" + indexPath + "'"
							+ " to trigger failures in tests.",
					e
			);
		}
	}

	@Override
	public void ensureFlushMergeRefreshOperationsFail(String indexName) {
		/*
		 * Flush:
		 * Lucene has optimizations in place to not apply flushes when there are no pending change in the writer.
		 * Thus, even if we ruthlessly delete the index from the filesystem,
		 * executing a flush will work most of the time,
		 * because most of the time changes are already committed when the flush executes.
		 *
		 * Merge:
		 * Lucene has optimizations in place to not apply mergeSegments() when there is only one segment.
		 * Thus, even if we ruthlessly delete the index from the filesystem,
		 * executing mergeSegments() will work most of the time,
		 * because most of the time we will only have one segment.
		 *
		 * Refresh:
		 * The refresh in the Lucene backend actually doesn't touch the index at all,
		 * so we don't have any way to trigger failures.
		 */
		throw new AssumptionViolatedException( "Cannot simulate flush/merge/refresh failures for the Lucene backend" );
	}

	public Directory openDirectory(String indexName) throws IOException {
		return FSDirectory.open( indexesPath.resolve( indexName ) );
	}

	private void deleteOrLogRecursively(Path path, String indexName) {
		try {
			Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					delete( path );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exception) {
					warnDeletionFailed( exception, file, indexName );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exception) {
					try {
						delete( path );
					}
					catch (IOException e) {
						exception = Throwables.combine( exception, e );
					}
					if ( exception != null ) {
						warnDeletionFailed( exception, dir, indexName );
					}
					return FileVisitResult.CONTINUE;
				}

			} );
		}
		catch (IOException e) {
			warnDeletionFailed( e, path, indexName );
		}
	}

	private void nukeOrLogIndexTopLevelDirectory(Path indexPath, String indexName) {
		// We don't want to delete the index directory itself, because HSearch will just re-create it.
		// Let's create invalid files, so that attempts to write will fail.
		for ( int i = 0; i < 20; i++ ) {
			createInvalidFile( indexPath, indexName, "_" + i + ".cfe" );
			createInvalidFile( indexPath, indexName, "_" + i + ".cfs" );
			createInvalidFile( indexPath, indexName, "_" + i + ".si" );
			createInvalidFile( indexPath, indexName, "segments_" + i );
		}
	}

	private void createInvalidFile(Path indexPath, String indexName, String fileName) {
		Path path = indexPath.resolve( fileName );
		try {
			try ( OutputStream os = Files.newOutputStream( path, StandardOpenOption.CREATE ) ) {
				os.write( "qwfbqfbwqfqw".getBytes( StandardCharsets.UTF_8 ) );
			}
		}
		catch (IOException e) {
			log.warnf(
					e,
					"Failed to create invalid file '%1$s' while corrupting index '%2$s'"
							+ " to trigger failures in tests."
							+ " Index corruption is incomplete and may not trigger failures "
							+ " depending on the current state of the Lucene backend.",
					path, indexName
			);
		}
	}

	private void delete(Path path) throws IOException {
		try {
			Files.delete( path );
		}
		catch (IOException e) {
			throw new IOException( "Could not delete '" + path + "'.", e );
		}
	}

	private void warnDeletionFailed(IOException exception, Path path, String indexName) {
		log.warnf(
				exception,
				"Failed to delete file '%1$s' while trying to corrupt index '%2$s'"
						+ " to trigger failures in tests."
						+ " Index corruption is incomplete and may not trigger failures "
						+ " depending on the current state of the Lucene backend.",
				path, indexName
		);
	}
}
