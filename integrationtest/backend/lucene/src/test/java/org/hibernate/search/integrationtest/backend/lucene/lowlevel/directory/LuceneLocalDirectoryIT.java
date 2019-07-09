/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

public class LuceneLocalDirectoryIT extends AbstractBuiltInDirectoryIT {

	/**
	 * Test that the index is created in the configured root.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void root() throws IOException {
		Path rootDirectory = temporaryFolder.getRoot().toPath();
		Path indexDirectory = rootDirectory.resolve( INDEX_NAME );

		assertThat( indexDirectory )
				.doesNotExist();

		setup( c -> c.withBackendProperty(
				BACKEND_NAME, LuceneBackendSettings.DIRECTORY_ROOT,
				temporaryFolder.getRoot().getAbsolutePath()
		) );

		assertThat( indexDirectory )
				.isDirectory();

		long contentSizeBeforeIndexing = directorySize( indexDirectory );

		checkIndexingAndQuerying();

		long contentSizeAfterIndexing = directorySize( indexDirectory );

		assertThat( contentSizeAfterIndexing )
				.isGreaterThan( contentSizeBeforeIndexing );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void filesystemAccessStrategy_default() {
		// The actual class used here is OS-dependent
		testFileSystemAccessStrategy( null, FSDirectory.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void filesystemAccessStrategy_auto() {
		// The actual class used here is OS-dependent
		testFileSystemAccessStrategy( "auto", FSDirectory.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.FSDirectorySelectionTest.testSimpleDirectoryType")
	public void filesystemAccessStrategy_simple() {
		testFileSystemAccessStrategy( "simple", SimpleFSDirectory.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.FSDirectorySelectionTest.testNIODirectoryType")
	public void filesystemAccessStrategy_nio() {
		testFileSystemAccessStrategy( "nio", NIOFSDirectory.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.FSDirectorySelectionTest.testMMapDirectoryType")
	public void filesystemAccessStrategy_mmap() {
		testFileSystemAccessStrategy( "mmap", MMapDirectory.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.FSDirectorySelectionTest.testInvalidDirectoryType")
	public void filesystemAccessStrategy_invalid() {
		SubTest.expectException( () -> setup( c -> c.withBackendProperty(
				BACKEND_NAME, LuceneBackendSettings.DIRECTORY_FILESYSTEM_ACCESS_STRATEGY,
				"some_invalid_name"
		) ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.backendContext( BACKEND_NAME )
						.failure(
								"Invalid filesystem access strategy name",
								"'some_invalid_name'",
								"Valid names are: [auto, simple, nio, mmap]"
						)
						.build()
				);
	}

	@Override
	protected Object getDirectoryType() {
		return "local-directory";
	}

	@Override
	protected String getDefaultLockClassName() {
		return NATIVE_FS_LOCK_FQN;
	}

	private void testFileSystemAccessStrategy(String strategyName,
			Class<? extends Directory> expectedDirectoryClass) {
		setup( c -> c.withBackendProperty(
				BACKEND_NAME, LuceneBackendSettings.DIRECTORY_FILESYSTEM_ACCESS_STRATEGY,
				strategyName
		) );

		checkIndexingAndQuerying();

		LuceneIndexManagerImpl luceneIndexManager = indexManager.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getIndexAccessorForTests().getDirectoryForTests() )
				.isInstanceOf( expectedDirectoryClass );
	}

	private static long directorySize(Path directory) throws IOException {
		return Files.walk( directory )
				.filter( p -> p.toFile().isFile() )
				.mapToLong( p -> p.toFile().length() )
				.sum();
	}
}
