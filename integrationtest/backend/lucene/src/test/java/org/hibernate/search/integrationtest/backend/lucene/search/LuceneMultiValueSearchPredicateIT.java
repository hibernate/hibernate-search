/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.SimpleIndexMapping;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.AssertionFailure;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * This is an extension of the backend TCK test
 * {@link LuceneMultiValueSearchPredicateIT}.
 */
@RunWith(Parameterized.class)
public class LuceneMultiValueSearchPredicateIT {

	private static final boolean SKIP_TEST_DOUBLE = false;
	private static final boolean SKIP_TEST_FLOAT = false;
	private static final boolean SKIP_TEST_LONG = false;
	private static final boolean SKIP_TEST_INTEGER = false;

	private static final boolean SKIP_TEST_PARENT = false;
	private static final boolean SKIP_TEST_FLATTENDED = false;
	private static final boolean SKIP_TEST_NESTED = false;
	private static final boolean SKIP_TEST_NESTED_WITH_FILTER = false;

	private final String VALUES = "data/search/multivalues-data.json";

	// Backend 1 / Index 1
	private static final String INDEX_NAME_1_1 = "IndexName_1_1";

	private static final String DOCUMENT_1_1_1 = "1_1_1";

	private static final String DOCUMENT_1_1_2 = "1_1_2";

	// Backend 1 / Index 2
	private static final String INDEX_NAME_1_2 = "IndexName_1_2";

	private static final String DOCUMENT_1_2_1 = "1_2_1";
	private static final String DOCUMENT_1_2_2 = "1_2_2";

	private final String directoryType;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	// Backend 1 / Index 1
	private IndexMapping_1_1 indexMapping_1_1;
	private StubMappingIndexManager indexManager_1_1;

	// Backend 1 / Index 2
	private IndexMapping_1_2 indexMapping_1_2;
	private StubMappingIndexManager indexManager_1_2;

	@Parameterized.Parameters(name = "Lucene directory type {0}")
	public static Object[] data() {
		return new Object[]{"local-heap", "local-filesystem"};
	}

	public LuceneMultiValueSearchPredicateIT(String directoryType) {
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
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void double_searchParentMultivalues() {

		if ( SKIP_TEST_PARENT ) {
			return;
		}

		if ( SKIP_TEST_DOUBLE ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalDoubleField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalDoubleField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalDoubleField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalDoubleField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalDoubleField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void float_searchParentMultivalues() {

		if ( SKIP_TEST_PARENT ) {
			return;
		}

		if ( SKIP_TEST_FLOAT ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalFloatField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalFloatField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalFloatField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalFloatField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalFloatField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void long_searchParentMultivalues() {

		if ( SKIP_TEST_PARENT ) {
			return;
		}

		if ( SKIP_TEST_LONG ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalLongField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalLongField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalLongField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalLongField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalLongField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void integer_searchParentMultivalues() {

		if ( SKIP_TEST_PARENT ) {
			return;
		}

		if ( SKIP_TEST_INTEGER ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalIntegerField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalIntegerField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalIntegerField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalIntegerField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "additionalIntegerField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void double_searchFlattendedMultivalues() {

		if ( SKIP_TEST_FLATTENDED ) {
			return;
		}

		if ( SKIP_TEST_DOUBLE ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalDoubleField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalDoubleField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalDoubleField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalDoubleField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalDoubleField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void float_searchFlattendedMultivalues() {

		if ( SKIP_TEST_FLATTENDED ) {
			return;
		}

		if ( SKIP_TEST_FLOAT ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalFloatField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalFloatField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalFloatField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalFloatField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalFloatField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void long_searchFlattendedMultivalues() {

		if ( SKIP_TEST_FLATTENDED ) {
			return;
		}

		if ( SKIP_TEST_LONG ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalLongField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalLongField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalLongField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalLongField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalLongField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void integer_searchFlattendedMultivalues() {

		if ( SKIP_TEST_FLATTENDED ) {
			return;
		}

		if ( SKIP_TEST_INTEGER ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalIntegerField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalIntegerField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalIntegerField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalIntegerField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "flattended.additionalIntegerField" ).asc().multi().median() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void double_searchNestedMultivalues() {

		if ( SKIP_TEST_NESTED ) {
			return;
		}

		if ( SKIP_TEST_DOUBLE ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalDoubleField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalDoubleField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalDoubleField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalDoubleField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void float_searchNestedMultivalues() {

		if ( SKIP_TEST_NESTED ) {
			return;
		}

		if ( SKIP_TEST_FLOAT ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalFloatField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalFloatField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalFloatField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalFloatField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void long_searchNestedMultivalues() {

		if ( SKIP_TEST_NESTED ) {
			return;
		}

		if ( SKIP_TEST_LONG ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalLongField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalLongField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalLongField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalLongField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void integer_searchNestedMultivalues() {

		if ( SKIP_TEST_NESTED ) {
			return;
		}

		if ( SKIP_TEST_INTEGER ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalIntegerField" ).asc().multi().min() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalIntegerField" ).asc().multi().max() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalIntegerField" ).asc().multi().avg() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

		query = scope.query()
			.where( f -> f.matchAll() )
			.sort( f -> f.field( "nested.additionalIntegerField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
		} );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3839")
	public void double_searchNestedMultivaluesWithFilter() {

		if ( SKIP_TEST_NESTED_WITH_FILTER ) {
			return;
		}

		if ( SKIP_TEST_DOUBLE ) {
			return;
		}

		StubMappingScope scope = indexManager_1_1.createScope();

		PredicateFinalStep filter = scope.predicate()
			.nested().objectField( "nested" ).nest( (f) -> {
			return f.match().field( "nested.active" ).matching( false );
		} );

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> {
				return f.bool().must( f.matchAll() )
					.filter( filter );
			} )
			.sort( f -> f.field( "nested.additionalIntegerField" ).asc().multi().sum() )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );
		} );

	}

	private void initData() {
		try {
			// Backend 1 / Index 1
			IndexIndexingPlan<? extends DocumentElement> plan = indexManager_1_1.createIndexingPlan();

			JSONObject dobj = LuceneIndexContentUtils.loadIndexData( VALUES );
			JSONObject dind1 = dobj.getJSONObject( INDEX_NAME_1_1 );

			LuceneIndexContentUtils.initIndexFromJson( dind1, indexMapping_1_1, plan );

			plan.execute().join();

			StubMappingScope scope = indexManager_1_1.createScope();
			SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
			assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );

			// Backend 1 / Index 2
			plan = indexManager_1_2.createIndexingPlan();

			JSONObject dind2 = dobj.getJSONObject( INDEX_NAME_1_2 );

			LuceneIndexContentUtils.initIndexFromJson( dind2, indexMapping_1_2, plan );

			plan.execute().join();

			scope = indexManager_1_2.createScope();
			query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
			assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_2, DOCUMENT_1_2_1, DOCUMENT_1_2_2 );

		}
		catch (JSONException ex) {
			throw new AssertionFailure( "init data", ex );
		}

	}

	private static class IndexMapping_1_1 extends SimpleIndexMapping {

		IndexMapping_1_1(IndexSchemaElement root) {

			add( "string", String.class, root.field(
				"string", f -> f.asString() )
				.toReference() );

			add( "double", Double.class, root.field(
				"additionalDoubleField",
				f -> f.asDouble().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "float", Float.class, root.field(
				"additionalFloatField",
				f -> f.asFloat().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "long", Long.class, root.field(
				"additionalLongField",
				f -> f.asLong().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "integer", Integer.class, root.field(
				"additionalIntegerField",
				f -> f.asInteger().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			//Add flattended index
			IndexSchemaObjectField fSubInd = root.objectField(
				"flattended", ObjectFieldStorage.FLATTENED );

			add( "flattended.double", Double.class, fSubInd.field(
				"additionalDoubleField",
				f -> f.asDouble().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "flattended.float", Float.class, fSubInd.field(
				"additionalFloatField",
				f -> f.asFloat().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "flattended.long", Long.class, fSubInd.field(
				"additionalLongField",
				f -> f.asLong().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "flattended.integer", Integer.class, fSubInd.field(
				"additionalIntegerField",
				f -> f.asInteger().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "flattended", ObjectFieldStorage.FLATTENED, fSubInd.toReference() );

			//Add nested index
			IndexSchemaObjectField nSubInd = root.objectField(
				"nested", ObjectFieldStorage.NESTED ).multiValued();

			add( "nested.active", Boolean.class, nSubInd.field(
				"active", f -> f.asBoolean() )
				.multiValued().toReference() );

			add( "nested.double", Double.class, nSubInd.field(
				"additionalDoubleField",
				f -> f.asDouble().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "nested.float", Float.class, nSubInd.field(
				"additionalFloatField",
				f -> f.asFloat().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "nested.long", Long.class, nSubInd.field(
				"additionalLongField",
				f -> f.asLong().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "nested.integer", Integer.class, nSubInd.field(
				"additionalIntegerField",
				f -> f.asInteger().sortable( Sortable.YES ) )
				.multiValued().toReference() );

			add( "nested", ObjectFieldStorage.NESTED, nSubInd.toReference() );

		}
	}

	private static class IndexMapping_1_2 extends SimpleIndexMapping {

		IndexMapping_1_2(IndexSchemaElement root) {
			add( "string", String.class, root.field(
				"string", f -> f.asString() )
				.toReference() );

		}
	}

}
