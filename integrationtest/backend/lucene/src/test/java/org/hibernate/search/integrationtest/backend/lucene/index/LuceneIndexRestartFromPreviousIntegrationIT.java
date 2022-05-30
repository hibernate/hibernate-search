/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.index;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LuceneIndexRestartFromPreviousIntegrationIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return Arrays.asList( "local-heap", "local-filesystem" );
	}

	@Parameterized.Parameter
	public String directoryType;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBindingV1> indexV1 = SimpleMappedIndex.of( IndexBindingV1::new );
	private final SimpleMappedIndex<IndexBindingV2> indexV2 = SimpleMappedIndex.of( IndexBindingV2::new );

	@Test
	public void addNewFieldOnExistingIndex() {
		StubMapping mappingV1 = setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY )
				.withIndex( indexV1 )
				.withBackendProperty( LuceneIndexSettings.DIRECTORY_TYPE, directoryType )
				.setup();

		BulkIndexer indexer1 = indexV1.bulkIndexer();
		indexer1.add( "1", doc -> {
			doc.addValue( indexV1.binding().name, "Fabio" );
		} );
		indexer1.join();

		assertThatQuery( indexV1.query().where( f -> f.match().field( "name" ).matching( "Fabio" ) ) )
				.hasDocRefHitsAnyOrder( indexV1.typeName(), "1" );

		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( indexV2 )
				.withBackendProperty( LuceneIndexSettings.DIRECTORY_TYPE, directoryType )
				.setup( mappingV1 );

		BulkIndexer indexer2 = indexV2.bulkIndexer();
		indexer2.add( "2", doc -> {
			doc.addValue( indexV2.binding().name, "Fabio" );
			doc.addValue( indexV2.binding().surname, "Ercoli" );
		} );
		indexer2.join();

		assertThatQuery( indexV2.query().where( f -> f.match().field( "surname" ).matching( "Ercoli" ) ) )
				.hasDocRefHitsAnyOrder( indexV2.typeName(), "2" );
		assertThatQuery( indexV2.query().where( f -> f.match().field( "name" ).matching( "Fabio" ) ) )
				.hasDocRefHitsAnyOrder( indexV2.typeName(), "1", "2" );
	}

	private static class IndexBindingV1 {
		final IndexFieldReference<String> name;

		IndexBindingV1(IndexSchemaElement root) {
			name = root.field( "name", c -> c.asString() ).toReference();
		}
	}

	private static class IndexBindingV2 {
		final IndexFieldReference<String> name;
		final IndexFieldReference<String> surname;

		IndexBindingV2(IndexSchemaElement root) {
			name = root.field( "name", c -> c.asString() ).toReference();
			surname = root.field( "surname", c -> c.asString() ).toReference();
		}
	}
}
