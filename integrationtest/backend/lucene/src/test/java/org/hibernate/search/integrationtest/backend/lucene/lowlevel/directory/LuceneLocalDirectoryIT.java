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
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.assertj.core.api.Assertions;

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
