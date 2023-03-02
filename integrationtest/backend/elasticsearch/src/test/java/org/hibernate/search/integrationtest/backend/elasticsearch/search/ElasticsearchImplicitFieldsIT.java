/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Map;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchImplicitFieldsIT {

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";
	private static final String EMPTY_ID = "empty";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@Before
	public void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup().integration();

		initData();
	}

	@Test
	public void implicit_fields_aggregation_entity_type() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<Map<String, Long>> countsByEntityKey = AggregationKey.of( "countsByEntity" );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( countsByEntityKey, f -> f.terms().field( "_entity_type", String.class ) )
				.toQuery();
		assertThatQuery( query ).aggregation( countsByEntityKey )
				.extracting( "mainType" )
				.isEqualTo( 6L )
		;
	}

	@Test
	public void implicit_fields_id() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.terms().field( "_id" ).matchingAny( "4" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( "mainType", "4" );
	}

	@Test
	public void implicit_fields_index() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "_index" ).matching( "main-000001" ) )
				.toQuery();
		assertThatQuery( query )
				.hasTotalHitCount( 6 )
				.hasDocRefHitsAnyOrder( "mainType", "1", "2", "3", "4", "5", "empty" );
	}

	private void initData() {
		mainIndex.bulkIndexer()
				.add( SECOND_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 2" );
				} )
				.add( FIRST_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 1" );
				} )
				.add( THIRD_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 3" );
				} )
				.add( FOURTH_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 4" );
				} )
				.add( FIFTH_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 5" );
				} )
				.add( EMPTY_ID, document -> {
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		}
	}
}
