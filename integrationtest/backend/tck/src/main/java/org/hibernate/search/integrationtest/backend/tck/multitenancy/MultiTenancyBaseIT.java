/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.multitenancy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.UUID;

import org.hibernate.search.engine.backend.common.DocumentReference;
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSession;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetupBeforeTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedPerClass
class MultiTenancyBaseIT {

	private static final String DOCUMENT_ID_1 = "1";
	private static final String DOCUMENT_ID_2 = "2";
	private static final String DOCUMENT_ID_3 = "3";

	private static final String STRING_VALUE_1 = "string_1";
	private static final String STRING_VALUE_2 = "string_2";
	private static final String STRING_VALUE_3 = "string_3";
	private static final String UPDATED_STRING = "updated_string";

	private static final Integer INTEGER_VALUE_1 = 1;
	private static final Integer INTEGER_VALUE_2 = 2;
	private static final Integer INTEGER_VALUE_3 = 3;
	private static final Integer INTEGER_VALUE_4 = 4;
	private static final Integer INTEGER_VALUE_5 = 5;

	private static final float[] FLOATS_VALUE_1 = new float[] { 1.0f, 1.0f };
	private static final float[] FLOATS_VALUE_2 = new float[] { -50.0f, -50.0f };
	private static final float[] FLOATS_VALUE_3 = new float[] { 1000.0f, 1000.0f };
	private static final float[] FLOATS_VALUE_4 = new float[] { 687.0f, 359.0f };

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private StubSession tenant1SessionContext;
	private StubSession tenant2SessionContext;

	private Object tenant1;
	private Object tenant2;
	private static StubMapping mapping;

	public static List<? extends Arguments> params() {
		return List.of(
				Arguments.of( "tenant_1", "tenant_2" ),
				Arguments.of( 1, 2 ),
				Arguments.of( UUID.fromString( "55555555-7777-6666-9999-000000000001" ),
						UUID.fromString( "55555555-7777-6666-9999-000000000002" ) )
		);
	}

	@BeforeAll
	static void beforeAll() {
		mapping = setupHelper.start( TckBackendHelper::createNoShardingMultiTenancyBackendSetupStrategy )
				.withIndex( index ).withMultiTenancy()
				.setup();
	}

	@ParameterizedSetup
	@MethodSource("params")
	void setup(Object tenant1, Object tenant2) {
		this.tenant1 = tenant1;
		this.tenant2 = tenant2;
	}

	@ParameterizedSetupBeforeTest
	void setup() {
		tenant1SessionContext = mapping.session( tenant1 );
		tenant2SessionContext = mapping.session( tenant2 );

		initData();
	}

	@Test
	void search_only_returns_elements_of_the_selected_tenant() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.match().field( "string" ).matching( STRING_VALUE_1 ) )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.match().field( "string" ).matching( STRING_VALUE_1 ) )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	// In Elasticsearch, we used to expect the user to provide the ID already prefixed with the tenant ID, which is wrong
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3421")
	void id_predicate_takes_tenantId_into_account() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.id().matching( DOCUMENT_ID_1 ) )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.id().matching( DOCUMENT_ID_1 ) )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	void search_on_nested_object_only_returns_elements_of_the_tenant() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.nested( "nestedObject" )
						.add( f.match()
								.field( "nestedObject.string" ).matching( STRING_VALUE_1 ) )
				)
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.nested( "nestedObject" )
						.add( f.match()
								.field( "nestedObject.string" ).matching( STRING_VALUE_1 ) )
				)
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	void delete_only_deletes_elements_of_the_tenant() {
		IndexIndexingPlan plan = index.createIndexingPlan( tenant2SessionContext );

		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query( tenant2SessionContext )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( projectionQuery ).hasListHitsAnyOrder( b -> {
			b.list( STRING_VALUE_1, INTEGER_VALUE_3 );
			b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		plan.delete( referenceProvider( DOCUMENT_ID_1 ) );

		plan.execute( OperationSubmitter.blocking() ).join();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_2 );
		projectionQuery = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( projectionQuery ).hasListHitsAnyOrder( b -> {
			b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		query = scope.query( tenant1SessionContext )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	void update_only_updates_elements_of_the_tenant() {
		IndexIndexingPlan plan = index.createIndexingPlan( tenant2SessionContext );

		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> checkQuery = scope.query( tenant2SessionContext )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( checkQuery )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2 );

		plan.addOrUpdate( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( index.binding().string, UPDATED_STRING );
			document.addValue( index.binding().integer, INTEGER_VALUE_4 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, UPDATED_STRING );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_4 );
		} );

		plan.execute( OperationSubmitter.blocking() ).join();

		// The tenant 2 has been updated properly.

		SearchQuery<List<?>> query = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.match().field( "string" ).matching( UPDATED_STRING ) )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( UPDATED_STRING, INTEGER_VALUE_4 ) );

		query = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.nested( "nestedObject" )
						.add( f.match()
								.field( "nestedObject.string" ).matching( UPDATED_STRING ) )
				)
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( UPDATED_STRING, INTEGER_VALUE_4 ) );

		// The tenant 1 has not been updated.

		query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.match().field( "nestedObject.string" ).matching( UPDATED_STRING ) )
				.toQuery();
		assertThatQuery( query ).hasNoHits();

		query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.nested( "nestedObject" )
						.add( f.match()
								.field( "nestedObject.string" ).matching( UPDATED_STRING ) )
				)
				.toQuery();
		assertThatQuery( query ).hasNoHits();

		query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.match().field( "string" ).matching( STRING_VALUE_1 ) )
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.nested( "nestedObject" )
						.add( f.match()
								.field( "nestedObject.string" ).matching( STRING_VALUE_1 ) )
				)
				.toQuery();
		assertThatQuery( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );
	}

	@Test
	void not_using_multi_tenancy_for_query_while_enabled_throws_exception() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.matchAll() )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Missing tenant identifier",
						"A tenant identifier is expected, because multi-tenancy is enabled for this backend" );
	}

	@Test
	void not_using_multi_tenancy_for_add_while_enabled_throws_exception() {
		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan();

			plan.add( referenceProvider( DOCUMENT_ID_3 ), document -> {
				document.addValue( index.binding().string, STRING_VALUE_3 );
				document.addValue( index.binding().integer, INTEGER_VALUE_5 );

				DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
				nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_3 );
				nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_5 );
			} );

			plan.execute( OperationSubmitter.blocking() ).join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Missing tenant identifier",
						"A tenant identifier is expected, because multi-tenancy is enabled for this backend" );
	}

	@Test
	void not_using_multi_tenancy_for_update_while_enabled_throws_exception() {
		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan();

			plan.addOrUpdate( referenceProvider( DOCUMENT_ID_2 ), document -> {
				document.addValue( index.binding().string, UPDATED_STRING );
				document.addValue( index.binding().integer, INTEGER_VALUE_4 );

				DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
				nestedObject.addValue( index.binding().nestedObject.string, UPDATED_STRING );
				nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_4 );
			} );

			plan.execute( OperationSubmitter.blocking() ).join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Missing tenant identifier",
						"A tenant identifier is expected, because multi-tenancy is enabled for this backend" );
	}

	@Test
	void not_using_multi_tenancy_for_delete_while_enabled_throws_exception() {
		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan();
			plan.delete( referenceProvider( DOCUMENT_ID_1 ) );
			plan.execute( OperationSubmitter.blocking() ).join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Missing tenant identifier",
						"A tenant identifier is expected, because multi-tenancy is enabled for this backend" );
	}

	@Test
	void searchingForVectors_vectorCloseToTheOneForTenant() {
		assumeTrue( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() );
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query( tenant1SessionContext )
				.where( f -> f.knn( 1 ).field( "floats" ).matching( FLOATS_VALUE_1 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1 );
	}

	@Test
	void searchingForVectors_moreElements() {
		assumeTrue( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.id(),
						f.field( "integer" )
				) )
				.where( f -> f.knn( 5 ).field( "floats" ).matching( FLOATS_VALUE_1 ) )
				.toQuery();


		assertThatQuery( query )
				.hits().hasHitsAnyOrder(
						List.of( DOCUMENT_ID_1, INTEGER_VALUE_3 ),
						List.of( DOCUMENT_ID_2, INTEGER_VALUE_4 )
				);
	}

	@Test
	void searchingForVectors_vectorCloseToTheOneForOtherTenant() {
		assumeTrue( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.id(),
						f.field( "integer" )
				) )
				.where( f -> f.knn( 1 ).field( "floats" ).matching( FLOATS_VALUE_3 ) )
				.toQuery();


		assertThatQuery( query )
				.hits().hasHitsAnyOrder( List.of( DOCUMENT_ID_1, INTEGER_VALUE_1 ) );
	}

	@Test
	void searchingForVectors_nested_vectorCloseToTheOneForOtherTenant() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().knnWorksInsideNestedPredicateWithImplicitFilters()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.id(),
						f.field( "integer" )
				) )
				.where( f -> f.knn( 1 ).field( "nestedObject.floats" ).matching( FLOATS_VALUE_3 ) )
				.toQuery();


		assertThatQuery( query )
				.hits().hasHitsAnyOrder( List.of( DOCUMENT_ID_1, INTEGER_VALUE_1 ) );
	}

	private void initData() {
		IndexIndexingPlan plan = index.createIndexingPlan( tenant1SessionContext );
		plan.addOrUpdate( referenceProvider( DOCUMENT_ID_1 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_1 );
			document.addValue( index.binding().integer, INTEGER_VALUE_1 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				document.addValue( index.binding().floats, FLOATS_VALUE_1 );
			}

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_1 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_1 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_1 );
			}
		} );

		plan.addOrUpdate( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_2 );
			document.addValue( index.binding().integer, INTEGER_VALUE_2 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				document.addValue( index.binding().floats, FLOATS_VALUE_2 );
			}

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_2 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_2 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_2 );
			}
		} );

		plan.execute( OperationSubmitter.blocking() ).join();

		plan = index.createIndexingPlan( tenant2SessionContext );
		plan.addOrUpdate( referenceProvider( DOCUMENT_ID_1 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_1 );
			document.addValue( index.binding().integer, INTEGER_VALUE_3 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				document.addValue( index.binding().floats, FLOATS_VALUE_3 );
			}

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_1 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_3 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_3 );
			}
		} );

		plan.addOrUpdate( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_2 );
			document.addValue( index.binding().integer, INTEGER_VALUE_4 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				document.addValue( index.binding().floats, FLOATS_VALUE_4 );
			}

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_2 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_4 );
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				nestedObject.addValue( index.binding().nestedObject.floats, FLOATS_VALUE_4 );
			}
		} );

		plan.execute( OperationSubmitter.blocking() ).join();

		// Check that all documents are searchable
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query( tenant1SessionContext )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = scope.query( tenant1SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( projectionQuery ).hasListHitsAnyOrder( b -> {
			b.list( STRING_VALUE_1, INTEGER_VALUE_1 );
			b.list( STRING_VALUE_2, INTEGER_VALUE_2 );
		} );

		query = scope.query( tenant2SessionContext )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_ID_1, DOCUMENT_ID_2 );

		projectionQuery = scope.query( tenant2SessionContext )
				.select( f -> f.composite(
						f.field( "string", String.class ),
						f.field( "integer", Integer.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( projectionQuery ).hasListHitsAnyOrder( b -> {
			b.list( STRING_VALUE_1, INTEGER_VALUE_3 );
			b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<float[]> floats;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				floats = root.field( "floats", f -> f.asFloatVector().dimension( 2 )
						.vectorSimilarity( VectorSimilarity.L2 )
						.projectable( Projectable.YES ) ).toReference();
			}
			else {
				floats = null;
			}
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectStructure.NESTED ).multiValued();
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;
		final IndexFieldReference<float[]> floats;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			integer = objectField.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				floats = objectField.field( "floats", f -> f.asFloatVector().dimension( 2 )
						.vectorSimilarity( VectorSimilarity.L2 )
						.projectable( Projectable.YES ) ).toReference();
			}
			else {
				floats = null;
			}
		}
	}
}
