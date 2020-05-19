/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.search.SearchMultiIndexIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
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

	// Index 1

	private static final String DOCUMENT_1_1 = "1_1";
	private static final String ADDITIONAL_FIELD_1_1 = "additional_field_1_1";

	private static final String DOCUMENT_1_2 = "1_2";
	private static final String ADDITIONAL_FIELD_1_2 = "additional_field_1_2";

	// Index 2

	private static final String DOCUMENT_2_1 = "2_1";

	private final String directoryType;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding1> index1 =
			SimpleMappedIndex.of( IndexBinding1::new ).name( "index1" );
	private final SimpleMappedIndex<IndexBinding2> index2 =
			SimpleMappedIndex.of( IndexBinding2::new ).name( "index2" );;

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
				.withIndexes( index1, index2 )
				.setup();

		initData();
	}

	@Test
	public void field_in_one_index_only_is_supported_for_sorting() {
		StubMappingScope scope = index1.createScope( index2 );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "additionalField" ).asc().missing().last() )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( index1.typeName(), DOCUMENT_1_1, DOCUMENT_1_2 );
			c.doc( index2.typeName(), DOCUMENT_2_1 );
		} );

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "additionalField" ).desc().missing().last() )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( index1.typeName(), DOCUMENT_1_2, DOCUMENT_1_1 );
			c.doc( index2.typeName(), DOCUMENT_2_1 );
		} );
	}

	private void initData() {
		BulkIndexer indexer1 = index1.bulkIndexer()
				.add( DOCUMENT_1_1, document -> {
					document.addValue( index1.binding().string, STRING_1 );
					document.addValue( index1.binding().additionalField, ADDITIONAL_FIELD_1_1 );
				} )
				.add( DOCUMENT_1_2, document -> {
					document.addValue( index1.binding().string, STRING_2 );
					document.addValue( index1.binding().additionalField, ADDITIONAL_FIELD_1_2 );
				} );
		BulkIndexer indexer2 = index2.bulkIndexer()
				.add( DOCUMENT_2_1, document -> {
					document.addValue( index2.binding().string, STRING_1 );
				} );
		indexer1.join( indexer2 );

		StubMappingScope scope = index1.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( index1.typeName(), DOCUMENT_1_1, DOCUMENT_1_2 );

		scope = index2.createScope();
		query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( index2.typeName(), DOCUMENT_2_1 );
	}

	private static class IndexBinding1 {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> additionalField;

		IndexBinding1(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
			additionalField = root.field(
					"additionalField",
					f -> f.asString().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
		}
	}

	private static class IndexBinding2 {
		final IndexFieldReference<String> string;

		IndexBinding2(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() )
					.toReference();
		}
	}
}
