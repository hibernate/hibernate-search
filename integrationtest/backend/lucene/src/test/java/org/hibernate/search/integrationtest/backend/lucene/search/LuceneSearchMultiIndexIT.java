/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.search.SearchMultiIndexIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This is an extension of the backend TCK test {@link SearchMultiIndexIT}.
 */
@RunWith(Parameterized.class)
public class LuceneSearchMultiIndexIT {

	private static final String STRING_1 = "string_1";
	private static final String STRING_2 = "string_2";

	// Backend 1 / Index 1

	private static final String INDEX_NAME_1_1 = "IndexName_1_1";

	private static final String DOCUMENT_1_1_1 = "1_1_1";
	private static final String ADDITIONAL_FIELD_1_1_1 = "additional_field_1_1_1";

	private static final String DOCUMENT_1_1_2 = "1_1_2";
	private static final String ADDITIONAL_FIELD_1_1_2 = "additional_field_1_1_2";

	// Backend 1 / Index 2

	private static final String INDEX_NAME_1_2 = "IndexName_1_2";

	private static final String DOCUMENT_1_2_1 = "1_2_1";

	private final String directoryType;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	// Backend 1 / Index 1

	private IndexMapping_1_1 indexMapping_1_1;
	private StubMappingIndexManager indexManager_1_1;

	// Backend 1 / Index 2

	private IndexMapping_1_2 indexMapping_1_2;
	private StubMappingIndexManager indexManager_1_2;

	@Parameters(name = "Lucene directory type {0}")
	public static Object[] data() {
		return new Object[] { "local-heap", "local-filesystem" };
	}

	public LuceneSearchMultiIndexIT(String directoryType) {
		this.directoryType = directoryType;
	}

	@Before
	public void setup() {
		setupHelper.start()
				.withBackendProperty( "directory.type", directoryType )
				.withIndex(
						INDEX_NAME_1_1,
						ctx -> this.indexMapping_1_1 = new IndexMapping_1_1( ctx.getSchemaElement() ),
						indexMapping -> this.indexManager_1_1 = indexMapping
				)
				.withIndex(
						INDEX_NAME_1_2,
						ctx -> this.indexMapping_1_2 = new IndexMapping_1_2( ctx.getSchemaElement() ),
						indexMapping -> this.indexManager_1_2 = indexMapping
				)
				.setup();

		initData();
	}

	@Test
	public void field_in_one_index_only_is_supported_for_sorting() {
		StubMappingScope scope = indexManager_1_1.createScope( indexManager_1_2 );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "additionalField" ).asc().missing().last() )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
		} );

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "additionalField" ).desc().missing().last() )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
		} );
	}

	private void initData() {
		// Backend 1 / Index 1

		IndexIndexingPlan plan = indexManager_1_1.createIndexingPlan();

		plan.add( referenceProvider( DOCUMENT_1_1_1 ), document -> {
			document.addValue( indexMapping_1_1.string, STRING_1 );
			document.addValue( indexMapping_1_1.additionalField, ADDITIONAL_FIELD_1_1_1 );
		} );
		plan.add( referenceProvider( DOCUMENT_1_1_2 ), document -> {
			document.addValue( indexMapping_1_1.string, STRING_2 );
			document.addValue( indexMapping_1_1.additionalField, ADDITIONAL_FIELD_1_1_2 );
		} );

		plan.execute().join();

		StubMappingScope scope = indexManager_1_1.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );

		// Backend 1 / Index 2

		plan = indexManager_1_2.createIndexingPlan();

		plan.add( referenceProvider( DOCUMENT_1_2_1 ), document -> {
			document.addValue( indexMapping_1_2.string, STRING_1 );
		} );

		plan.execute().join();

		scope = indexManager_1_2.createScope();
		query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
	}

	private static class IndexMapping_1_1 {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> additionalField;

		IndexMapping_1_1(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
			additionalField = root.field(
					"additionalField",
					f -> f.asString().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
		}
	}

	private static class IndexMapping_1_2 {
		final IndexFieldReference<String> string;

		IndexMapping_1_2(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() )
					.toReference();
		}
	}
}
