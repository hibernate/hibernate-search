/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.assertj.core.api.Assertions;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

public class LuceneLocalDirectoryIT {

	private static final String BACKEND_NAME = "BackendName";
	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	/**
	 * Test that the index is created in the configured root.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void root() throws IOException {
		Path rootDirectory = temporaryFolder.getRoot().toPath();
		Path indexDirectory = rootDirectory.resolve( INDEX_NAME );

		Assertions.assertThat( indexDirectory )
				.doesNotExist();

		setup( c -> c.withBackendProperty(
				BACKEND_NAME, LuceneBackendSettings.DIRECTORY_ROOT,
				temporaryFolder.getRoot().getAbsolutePath()
		) );

		Assertions.assertThat( indexDirectory )
				.isDirectory();

		long contentSizeBeforeIndexing = directorySize( indexDirectory );

		checkIndexingAndQuerying();

		long contentSizeAfterIndexing = directorySize( indexDirectory );

		Assertions.assertThat( contentSizeAfterIndexing )
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

	private void testFileSystemAccessStrategy(String strategyName,
			Class<? extends Directory> expectedDirectoryClass) {
		setup( c -> c.withBackendProperty(
				BACKEND_NAME, LuceneBackendSettings.DIRECTORY_FILESYSTEM_ACCESS_STRATEGY,
				strategyName
		) );

		checkIndexingAndQuerying();

		LuceneIndexManagerImpl luceneIndexManager = indexManager.unwrapForTests( LuceneIndexManagerImpl.class );
		Assertions.assertThat( luceneIndexManager.getIndexAccessorForTests().getDirectoryForTests() )
				.isInstanceOf( expectedDirectoryClass );
	}

	private void checkIndexingAndQuerying() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.string, "text 1" );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.string, "text 2" );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.string, "text 3" );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder(
				INDEX_NAME,
				DOCUMENT_1, DOCUMENT_2, DOCUMENT_3
		);
	}

	private void setup(Function<SearchSetupHelper.SetupContext, SearchSetupHelper.SetupContext> additionalConfiguration) {
		additionalConfiguration.apply(
				setupHelper.start( BACKEND_NAME )
						.withIndex(
								INDEX_NAME,
								ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
								indexManager -> this.indexManager = indexManager
						)
						.withBackendProperty(
								BACKEND_NAME, LuceneBackendSettings.DIRECTORY_TYPE, "local-directory"
						)
		)
				.setup();
	}

	private static long directorySize(Path directory) throws IOException {
		return Files.walk( directory )
				.filter( p -> p.toFile().isFile() )
				.mapToLong( p -> p.toFile().length() )
				.sum();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString()
			)
					.toReference();
		}
	}
}
