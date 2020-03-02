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

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

import org.junit.rules.TemporaryFolder;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.assertj.core.api.iterable.ThrowingExtractor;

public final class LuceneIndexContentUtils {

	private LuceneIndexContentUtils() {
	}

	public static <T> T doOnIndexCopy(SearchSetupHelper setupHelper, TemporaryFolder temporaryFolder,
		String indexName, ThrowingExtractor<DirectoryReader, T, IOException> action) throws IOException {
		Path indexCopyPath = temporaryFolder.getRoot().toPath().resolve( indexName + "_copy" );

		T result;
		LuceneTckBackendAccessor accessor = (LuceneTckBackendAccessor) setupHelper.getBackendAccessor();
		try {
			// Copy the index to be able to open a directory despite the lock
			accessor.copyIndexContent( indexCopyPath, indexName );

			try ( Directory directory = FSDirectory.open( indexCopyPath );
				DirectoryReader reader = DirectoryReader.open( directory ) ) {
				result = action.apply( reader );
			}
		}
		finally {
			try {
				deleteRecursively( indexCopyPath );
			}
			catch (RuntimeException | IOException e) {
				System.out.println( "Could not delete '" + indexCopyPath + "': " + e );
			}
		}

		return result;
	}

	public static void deleteRecursively(Path path) throws IOException {
		Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete( file );
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists( dir );
				return FileVisitResult.CONTINUE;
			}
		} );
	}
}
