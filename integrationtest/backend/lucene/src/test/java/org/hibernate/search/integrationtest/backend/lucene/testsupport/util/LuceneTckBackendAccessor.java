/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;

class LuceneTckBackendAccessor implements TckBackendAccessor {
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
			Files.walkFileTree( indexPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete( file );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete( dir );
					return FileVisitResult.CONTINUE;
				}
			} );
			// Create a non-directory fail to prevent HSearch from re-creating the index automatically.
			Files.createFile( indexPath );
		}
		catch (RuntimeException | IOException e) {
			throw new IllegalStateException(
					"Unexpected exception deleting index '" + indexName + "' at path '" + indexPath + "'", e
			);
		}
	}
}
