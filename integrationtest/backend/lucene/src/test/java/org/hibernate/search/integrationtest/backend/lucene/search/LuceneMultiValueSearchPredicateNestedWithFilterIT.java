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
import org.hibernate.search.engine.search.sort.dsl.SortMultiValue;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.JSONTestModelLoader;
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
public class LuceneMultiValueSearchPredicateNestedWithFilterIT {

	private final String VALUES = "data/search/multivalues-nested-data.json";

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

	public LuceneMultiValueSearchPredicateNestedWithFilterIT(String directoryType) {
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
	public void double_searchNestedMultivaluesWithFilter() {

		StubMappingScope scope = indexManager_1_1.createScope();

		PredicateFinalStep filter = scope.predicate()
			.nested().objectField( "nested" ).nest( (f) -> {
			return f.match().field( "nested.active" ).matching( true );
		} );

		SearchQuery<DocumentReference> query = scope.query()
			.where( f -> {
				return f.bool().must( f.matchAll() )
					.must( filter );
			} )
			.sort( f -> f.field( "nested.additionalIntegerField" )
			.asc().multi( SortMultiValue.SUM ) )
			.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );
		} );

	}

	private void initData() {
		try {
			// Backend 1 / Index 1
			IndexIndexingPlan<? extends DocumentElement> plan = indexManager_1_1.createIndexingPlan();

			JSONObject dobj = JSONTestModelLoader.loadIndexData( VALUES );
			JSONObject dind1 = dobj.getJSONObject( INDEX_NAME_1_1 );

			JSONTestModelLoader.initIndexFromJson( dind1, indexMapping_1_1, plan );

			plan.execute().join();

			StubMappingScope scope = indexManager_1_1.createScope();
			SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
			assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );

			// Backend 1 / Index 2
			plan = indexManager_1_2.createIndexingPlan();

			JSONObject dind2 = dobj.getJSONObject( INDEX_NAME_1_2 );

			JSONTestModelLoader.initIndexFromJson( dind2, indexMapping_1_2, plan );

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
