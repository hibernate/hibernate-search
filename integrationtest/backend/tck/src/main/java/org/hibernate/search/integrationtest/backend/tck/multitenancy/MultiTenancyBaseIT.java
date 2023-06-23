/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.multitenancy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSession;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MultiTenancyBaseIT {

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
	public final SearchSetupHelper setupHelper =
			new SearchSetupHelper( TckBackendHelper::createMultiTenancyBackendSetupStrategy );

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private StubSession tenant1SessionContext;
	private StubSession tenant2SessionContext;

	@Before
	public void setup() {
		StubMapping mapping = setupHelper.start().withIndex( index ).withMultiTenancy().setup();

		tenant1SessionContext = mapping.session( TENANT_1 );
		tenant2SessionContext = mapping.session( TENANT_2 );

		initData();
	}

	@Test
	public void search_only_returns_elements_of_the_selected_tenant() {
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
	public void id_predicate_takes_tenantId_into_account() {
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
	public void search_on_nested_object_only_returns_elements_of_the_tenant() {
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
	public void delete_only_deletes_elements_of_the_tenant() {
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
	public void update_only_updates_elements_of_the_tenant() {
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
	public void not_using_multi_tenancy_for_query_while_enabled_throws_exception() {
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
	public void not_using_multi_tenancy_for_add_while_enabled_throws_exception() {
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
	public void not_using_multi_tenancy_for_update_while_enabled_throws_exception() {
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
	public void not_using_multi_tenancy_for_delete_while_enabled_throws_exception() {
		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan();
			plan.delete( referenceProvider( DOCUMENT_ID_1 ) );
			plan.execute( OperationSubmitter.blocking() ).join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Missing tenant identifier",
						"A tenant identifier is expected, because multi-tenancy is enabled for this backend" );
	}

	private void initData() {
		IndexIndexingPlan plan = index.createIndexingPlan( tenant1SessionContext );
		plan.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_1 );
			document.addValue( index.binding().integer, INTEGER_VALUE_1 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_1 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_1 );
		} );

		plan.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_2 );
			document.addValue( index.binding().integer, INTEGER_VALUE_2 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_2 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_2 );
		} );

		plan.execute( OperationSubmitter.blocking() ).join();

		plan = index.createIndexingPlan( tenant2SessionContext );
		plan.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_1 );
			document.addValue( index.binding().integer, INTEGER_VALUE_3 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_1 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_3 );
		} );

		plan.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			document.addValue( index.binding().string, STRING_VALUE_2 );
			document.addValue( index.binding().integer, INTEGER_VALUE_4 );

			DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
			nestedObject.addValue( index.binding().nestedObject.string, STRING_VALUE_2 );
			nestedObject.addValue( index.binding().nestedObject.integer, INTEGER_VALUE_4 );
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
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectStructure.NESTED ).multiValued();
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
