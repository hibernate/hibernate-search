/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RoutingVectorSearchIT {

	private static final String ROUTING_KEY_1 = "routingKey1";
	private static final String ROUTING_KEY_2 = "routingKey2";
	private static final String DOCUMENT_ID_1 = "1";
	private static final String DOCUMENT_ID_2 = "2";
	private static final String DOCUMENT_ID_3 = "3";
	private static final String DOCUMENT_ID_4 = "4";

	private static final Integer INTEGER_VALUE_1 = 1;
	private static final Integer INTEGER_VALUE_2 = 2;
	private static final Integer INTEGER_VALUE_3 = 3;
	private static final Integer INTEGER_VALUE_4 = 4;

	private static final float[] FLOATS_VALUE_1 = new float[] { 1.0f, 1.0f };
	private static final float[] FLOATS_VALUE_2 = new float[] { -50.0f, -50.0f };
	private static final float[] FLOATS_VALUE_3 = new float[] { 1000.0f, 1000.0f };
	private static final float[] FLOATS_VALUE_4 = new float[] { 687.0f, 359.0f };

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void beforeAll() {
		assumeTrue( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() );
	}

	@BeforeEach
	void setup() {
		StubMapping mapping = setupHelper.start()
				.withIndex( index )
				.setup();

		IndexIndexingPlan plan = index.createIndexingPlan( mapping.session() );
		plan.add( referenceProvider( DOCUMENT_ID_1, ROUTING_KEY_1 ), document -> {
			document.addValue( index.binding().integer, INTEGER_VALUE_1 );
			document.addValue( index.binding().floats, FLOATS_VALUE_1 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_1 );
		} );

		plan.add( referenceProvider( DOCUMENT_ID_2, ROUTING_KEY_1 ), document -> {
			document.addValue( index.binding().integer, INTEGER_VALUE_2 );
			document.addValue( index.binding().floats, FLOATS_VALUE_2 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_2 );
		} );

		plan.add( referenceProvider( DOCUMENT_ID_3, ROUTING_KEY_2 ), document -> {
			document.addValue( index.binding().integer, INTEGER_VALUE_3 );
			document.addValue( index.binding().floats, FLOATS_VALUE_3 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_3 );
		} );

		plan.add( referenceProvider( DOCUMENT_ID_4, ROUTING_KEY_2 ), document -> {
			document.addValue( index.binding().integer, INTEGER_VALUE_4 );
			document.addValue( index.binding().floats, FLOATS_VALUE_4 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_4 );
		} );

		plan.execute( OperationSubmitter.blocking() ).join();
	}

	@Test
	void searchingForVectors_moreElements() {
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.knn( 5 ).field( "floats" ).matching( FLOATS_VALUE_1 ) )
				.routing( ROUTING_KEY_1 )
				.toQuery();

		assertThatQuery( query )
				.hits().hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2 );

		query = scope.query()
				.where( f -> f.knn( 5 ).field( "floats" ).matching( FLOATS_VALUE_1 ) )
				.routing( ROUTING_KEY_1 )
				.routing( ROUTING_KEY_2 )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2, DOCUMENT_ID_3, DOCUMENT_ID_4 );
	}

	@Test
	void searchingForVectors_vectorCloseToTheOneForOtherRoute() {
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.knn( 1 ).field( "floats" ).matching( FLOATS_VALUE_3 ) )
				.routing( ROUTING_KEY_1 )
				.toQuery();


		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1 );
	}

	@Test
	void searchingForVectors_nested_vectorCloseToTheOneForOtherRoute() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().knnWorksInsideNestedPredicateWithImplicitFilters()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.knn( 1 ).field( "nestedObject.floats" ).matching( FLOATS_VALUE_3 ) )
				.routing( ROUTING_KEY_1 )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1 );

		query = scope.query()
				.where( f -> f.nested( "nestedObject" ).add(
						f.knn( 1 ).field( "nestedObject.floats" ).matching( FLOATS_VALUE_3 )
				) )
				.routing( ROUTING_KEY_1 )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1 );
	}

	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<float[]> floats;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
			floats = root.field( "floats", f -> f.asFloatVector().dimension( 2 ).vectorSimilarity( VectorSimilarity.L2 )
					.projectable( Projectable.YES ) ).toReference();
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectStructure.NESTED ).multiValued();
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<float[]> floats;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			floats = objectField.field( "floats", f -> f.asFloatVector().dimension( 2 ).vectorSimilarity( VectorSimilarity.L2 )
					.projectable( Projectable.YES ) ).toReference();
		}
	}
}
