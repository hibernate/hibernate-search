/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractDirectoryIT {

	protected static final String MAPPED_TYPE_NAME = "MappedTypeName";
	protected static final String BACKEND_NAME = "BackendName";
	protected static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected SearchIntegration searchIntegration;
	protected IndexMapping indexMapping;
	protected StubMappingIndexManager indexManager;

	protected final void checkIndexingAndQuerying() {
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

	protected final void setup(Object directoryType,
			Function<SearchSetupHelper.SetupContext, SearchSetupHelper.SetupContext> additionalConfiguration) {
		searchIntegration = additionalConfiguration.apply(
				setupHelper.start( BACKEND_NAME )
						.withIndex(
								INDEX_NAME,
								c -> c.mappedType( MAPPED_TYPE_NAME ),
								ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
								indexManager -> this.indexManager = indexManager
						)
						.withBackendProperty(
								BACKEND_NAME, LuceneBackendSettings.DIRECTORY_TYPE, directoryType
						)
		)
				.setup();
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
