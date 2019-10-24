/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.util.common.impl.Throwables;

import org.jboss.logging.Logger;

class LuceneTckBackendAccessor implements TckBackendAccessor {

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
	public void ensureIndexOperationsFail(String indexName) {
		Path indexPath = indexesPath.resolve( indexName );
		try {
			// Try to remove the content of the index, or to prevent future access to the files
			// if we can't delete them (can happen on windows).
			Files.list( indexPath ).forEach( path -> nukeOrLogRecursively( path, indexName ) );
			// We don't want to delete the index directory itself, because HSearch will just re-create it.
			// Try to remove all permissions, which will prevent HSearch from creating new content.
			makeInaccessible( indexPath );
		}
		catch (RuntimeException | IOException e) {
			throw new IllegalStateException(
					"Unexpected exception while deleting and changing files in index '"
							+ indexName + "' at path '" + indexPath + "'"
							+ " to trigger failures in tests.", e
			);
		}
	}

	private void nukeOrLogRecursively(Path path, String indexName) {
		try {
			Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					nukeOrLog( file, indexName, null );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exception) {
					nukeOrLog( file, indexName, exception );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exception) {
					nukeOrLog( dir, indexName, exception );
					return FileVisitResult.CONTINUE;
				}
			} );
		}
		catch (IOException e) {
			warnNukeFailed( e, path, indexName );
		}
	}

	private void nukeOrLog(Path path, String indexName, IOException previousException) {
		IOException exception = previousException;
		try {
			// Try to delete it...
			delete( path );
			return; // Success, don't worry about the rest
		}
		catch (IOException e) {
			exception = Throwables.combine( exception, e );
		}

		// If we can't delete it, try to make it inaccessible.
		// This may also prevent HSearch from re-creating the index automatically.
		try {
			makeInaccessible( path );
			return; // Success, don't worry about the rest
		}
		catch (IOException e) {
			exception = Throwables.combine( exception, e );
		}

		warnNukeFailed( exception, path, indexName );
	}

	private void delete(Path path) throws IOException {
		try {
			Files.delete( path );
		}
		catch (IOException e) {
			throw new IOException( "Could not delete '" + path + "'.", e );
		}
	}

	private void makeInaccessible(Path path) throws IOException {
		if ( ! path.toFile().setReadOnly() ) {
			throw new IOException( "Could not make '" + path + "' inaccessible." );
		}
	}

	private void warnNukeFailed(IOException exception, Path path, String indexName) {
		log.warnf(
				exception,
				"Failed to access file '%1$s' while deleting index '%2$s'"
						+ " to trigger failures in tests."
						+ " Index deletion is incomplete and may not trigger failures "
						+ " depending on the current state of the Lucene backend.",
				path, indexName
		);
	}
}
