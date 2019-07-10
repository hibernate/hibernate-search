/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.multitenancy;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MultiTenancyBaseIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String TENANT_1 = "tenant_1";
	private static final String TENANT_2 = "tenant_2";

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

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper( TckBackendHelper::createMultiTenancyBackendSetupStrategy );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final StubSessionContext tenant1SessionContext = new StubSessionContext( TENANT_1 );
	private final StubSessionContext tenant2SessionContext = new StubSessionContext( TENANT_2 );

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withMultiTenancy()
				.setup();

		initData();
	}

	@Test
	public void search_only_returns_elements_of_the_selected_tenant() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE_1 ) )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE_1 ) )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	// In Elasticsearch, we used to expect the user to provide the ID already prefixed with the tenant ID, which is wrong
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3421")
	public void id_predicate_takes_tenantId_into_account() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.id().matching( DOCUMENT_ID_1 ) )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.id().matching( DOCUMENT_ID_1 ) )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	public void search_on_nested_object_only_returns_elements_of_the_tenant() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.match()
								.onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
						)
				)
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.match()
								.onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
						)
				)
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	public void delete_only_deletes_elements_of_the_tenant() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant2SessionContext );

		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query( tenant2SessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_1, INTEGER_VALUE_3 );
				b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		workPlan.delete( referenceProvider( DOCUMENT_ID_1 ) );

		workPlan.execute().join();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_2 );
		projectionQuery = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		query = scope.query( tenant1SessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void update_only_updates_elements_of_the_tenant() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant2SessionContext );

		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> checkQuery = scope.query( tenant2SessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( checkQuery )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		workPlan.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( indexMapping.string, UPDATED_STRING );
			document.addValue( indexMapping.integer, INTEGER_VALUE_4 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, UPDATED_STRING );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_4 );
		} );

		workPlan.execute().join();

		// The tenant 2 has been updated properly.

		SearchQuery<List<?>> query = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( UPDATED_STRING ) )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( UPDATED_STRING, INTEGER_VALUE_4 ) );

		query = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.match()
								.onField( "nestedObject.string" ).matching( UPDATED_STRING )
						)
				)
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( UPDATED_STRING, INTEGER_VALUE_4 ) );

		// The tenant 1 has not been updated.

		query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "nestedObject.string" ).matching( UPDATED_STRING ) )
				.toQuery();
		assertThat( query ).hasNoHits();

		query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.match()
								.onField( "nestedObject.string" ).matching( UPDATED_STRING )
						)
				)
				.toQuery();
		assertThat( query ).hasNoHits();

		query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE_1 ) )
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.match()
								.onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
						)
				)
				.toQuery();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );
	}

	@Test
	public void not_using_multi_tenancy_for_query_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query( new StubSessionContext() )
				.predicate( f -> f.matchAll() )
				.toQuery();
	}

	@Test
	public void not_using_multi_tenancy_for_add_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( new StubSessionContext() );

		workPlan.add( referenceProvider( DOCUMENT_ID_3 ), document -> {
			document.addValue( indexMapping.string, STRING_VALUE_3 );
			document.addValue( indexMapping.integer, INTEGER_VALUE_5 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, STRING_VALUE_3 );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_5 );
		} );

		workPlan.execute().join();
	}

	@Test
	public void not_using_multi_tenancy_for_update_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( new StubSessionContext() );

		workPlan.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( indexMapping.string, UPDATED_STRING );
			document.addValue( indexMapping.integer, INTEGER_VALUE_4 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, UPDATED_STRING );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_4 );
		} );

		workPlan.execute().join();
	}

	@Test
	public void not_using_multi_tenancy_for_delete_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( new StubSessionContext() );
		workPlan.delete( referenceProvider( DOCUMENT_ID_1 ) );
		workPlan.execute().join();
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );
		workPlan.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			document.addValue( indexMapping.string, STRING_VALUE_1 );
			document.addValue( indexMapping.integer, INTEGER_VALUE_1 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, STRING_VALUE_1 );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_1 );
		} );

		workPlan.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( indexMapping.string, STRING_VALUE_2 );
			document.addValue( indexMapping.integer, INTEGER_VALUE_2 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, STRING_VALUE_2 );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_2 );
		} );

		workPlan.execute().join();

		workPlan = indexManager.createWorkPlan( tenant2SessionContext );
		workPlan.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			document.addValue( indexMapping.string, STRING_VALUE_1 );
			document.addValue( indexMapping.integer, INTEGER_VALUE_3 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, STRING_VALUE_1 );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_3 );
		} );

		workPlan.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( indexMapping.string, STRING_VALUE_2 );
			document.addValue( indexMapping.integer, INTEGER_VALUE_4 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, STRING_VALUE_2 );
			nestedObject.addValue( indexMapping.nestedObject.integer, INTEGER_VALUE_4 );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query( tenant1SessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = scope.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_1, INTEGER_VALUE_1 );
				b.list( STRING_VALUE_2, INTEGER_VALUE_2 );
		} );

		query = scope.query( tenant2SessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		projectionQuery = scope.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_1, INTEGER_VALUE_3 );
				b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<Integer> integer;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED ).multiValued();
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			integer = objectField.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
		}
	}
}
