/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchImplicitFieldsIT {

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";
	private static final String EMPTY_ID = "empty";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup().integration();

		initData();
	}

	@Test
	void implicit_fields_aggregation_entity_type() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<Map<String, Long>> countsByEntityKey = AggregationKey.of( "countsByEntity" );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( countsByEntityKey, f -> f.terms().field( "_entity_type", String.class ) )
				.toQuery();
		assertThatQuery( query ).aggregation( countsByEntityKey )
				.extracting( "mainType" )
				.isEqualTo( 6L );
	}

	@Test
	void implicit_fields_id() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.terms().field( "_id" ).matchingAny( "4" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( "mainType", "4" );
	}

	@Test
	void implicit_fields_index() {
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
				.add( EMPTY_ID, document -> {} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		}
	}
}
