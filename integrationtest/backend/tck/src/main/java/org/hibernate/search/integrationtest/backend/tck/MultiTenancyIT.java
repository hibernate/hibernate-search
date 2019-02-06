/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MultiTenancyIT {

	public static final String CONFIGURATION_ID = "multi-tenancy";

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
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final StubSessionContext tenant1SessionContext = new StubSessionContext( TENANT_1 );
	private final StubSessionContext tenant2SessionContext = new StubSessionContext( TENANT_2 );

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withConfiguration( CONFIGURATION_ID )
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withMultiTenancy()
				.setup();

		initData();
	}

	@Test
	public void search_only_returns_elements_of_the_selected_tenant() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE_1 ) )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE_1 ) )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	// In Elasticsearch, we used to expect the user to provide the ID already prefixed with the tenant ID, which is wrong
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3421")
	public void id_predicate_takes_tenantId_into_account() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.id().matching( DOCUMENT_ID_1 ) )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.id().matching( DOCUMENT_ID_1 ) )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	public void search_on_nested_object_only_returns_elements_of_the_tenant() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query( tenant1SessionContext )
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
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								searchTarget.projection().field( "string", String.class ),
								searchTarget.projection().field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.match()
								.onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
						)
				)
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	public void delete_only_deletes_elements_of_the_tenant() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant2SessionContext );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query( tenant2SessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_1, INTEGER_VALUE_3 );
				b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		workPlan.delete( referenceProvider( DOCUMENT_ID_1 ) );

		workPlan.execute().join();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_2 );
		projectionQuery = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		query = searchTarget.query( tenant1SessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void update_only_updates_elements_of_the_tenant() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant2SessionContext );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> checkQuery = searchTarget.query( tenant2SessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( checkQuery )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		workPlan.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, UPDATED_STRING );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, UPDATED_STRING );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		workPlan.execute().join();

		// The tenant 2 has been updated properly.

		SearchQuery<List<?>> query = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( UPDATED_STRING ) )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( UPDATED_STRING, INTEGER_VALUE_4 ) );

		query = searchTarget.query( tenant2SessionContext )
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
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( UPDATED_STRING, INTEGER_VALUE_4 ) );

		// The tenant 1 has not been updated.

		query = searchTarget.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "nestedObject.string" ).matching( UPDATED_STRING ) )
				.build();
		assertThat( query ).hasNoHits();

		query = searchTarget.query( tenant1SessionContext )
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
				.build();
		assertThat( query ).hasNoHits();

		query = searchTarget.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE_1 ) )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant1SessionContext )
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
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> b.list( STRING_VALUE_1, INTEGER_VALUE_1 ) );
	}

	@Test
	public void backend_multi_tenancy_disabled_but_indexes_requiring_multi_tenancy_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Index" );
		thrown.expectMessage( "requires multi-tenancy but the backend" );
		thrown.expectMessage( "does not support it in its current configuration." );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withMultiTenancy()
				.setup();

		initData();
	}

	@Test
	public void using_multi_tenancy_for_query_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_query_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query( tenant1SessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void using_multi_tenancy_for_add_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_add_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );

		workPlan.add( referenceProvider( DOCUMENT_ID_3 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_3 );
			indexAccessors.integer.write( document, INTEGER_VALUE_5 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_3 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_5 );
		} );

		workPlan.execute().join();
	}

	@Test
	public void using_multi_tenancy_for_update_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_update_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );

		workPlan.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, UPDATED_STRING );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, UPDATED_STRING );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		workPlan.execute().join();
	}

	@Test
	public void using_multi_tenancy_for_delete_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_delete_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );
		workPlan.delete( referenceProvider( DOCUMENT_ID_1 ) );
		workPlan.execute().join();
	}

	@Test
	public void not_using_multi_tenancy_for_query_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query( new StubSessionContext() )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void not_using_multi_tenancy_for_add_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( new StubSessionContext() );

		workPlan.add( referenceProvider( DOCUMENT_ID_3 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_3 );
			indexAccessors.integer.write( document, INTEGER_VALUE_5 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_3 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_5 );
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
			indexAccessors.string.write( document, UPDATED_STRING );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, UPDATED_STRING );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
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
			indexAccessors.string.write( document, STRING_VALUE_1 );
			indexAccessors.integer.write( document, INTEGER_VALUE_1 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_1 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_1 );
		} );

		workPlan.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_2 );
			indexAccessors.integer.write( document, INTEGER_VALUE_2 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_2 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_2 );
		} );

		workPlan.execute().join();

		workPlan = indexManager.createWorkPlan( tenant2SessionContext );
		workPlan.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_1 );
			indexAccessors.integer.write( document, INTEGER_VALUE_3 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_1 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_3 );
		} );

		workPlan.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_2 );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_2 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query( tenant1SessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = searchTarget.query( tenant1SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_1, INTEGER_VALUE_1 );
				b.list( STRING_VALUE_2, INTEGER_VALUE_2 );
		} );

		query = searchTarget.query( tenant2SessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		projectionQuery = searchTarget.query( tenant2SessionContext )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.field( "integer", Integer.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( projectionQuery ).hasListHitsAnyOrder( b -> {
				b.list( STRING_VALUE_1, INTEGER_VALUE_3 );
				b.list( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<Integer> integer;
		final ObjectAccessors nestedObject;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.createAccessor();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.createAccessor();
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectAccessors( nestedObjectField );
		}
	}

	private static class ObjectAccessors {
		final IndexObjectFieldAccessor self;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<String> string;

		ObjectAccessors(IndexSchemaObjectField objectField) {
			self = objectField.createAccessor();
			string = objectField.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.createAccessor();
			integer = objectField.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.createAccessor();
		}
	}
}
