/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractDirectoryIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	protected SearchIntegration searchIntegration;

	protected final void checkIndexingAndQuerying() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( index.binding().string, "text 1" );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( index.binding().string, "text 2" );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( index.binding().string, "text 3" );
		} );
		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder(
				index.typeName(),
				DOCUMENT_1, DOCUMENT_2, DOCUMENT_3
		);
	}

	protected final void setup(Object directoryType,
			Function<SearchSetupHelper.SetupContext, SearchSetupHelper.SetupContext> additionalConfiguration) {
		searchIntegration = additionalConfiguration.apply(
				setupHelper.start()
						.withIndex( index )
						.withBackendProperty(
								LuceneIndexSettings.DIRECTORY_TYPE, directoryType
						)
		)
				.setup();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString()
			)
					.toReference();
		}
	}
}
