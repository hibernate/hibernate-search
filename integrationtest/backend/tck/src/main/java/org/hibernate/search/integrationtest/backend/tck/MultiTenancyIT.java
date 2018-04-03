/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.ProjectionsSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MultiTenancyIT {

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

	private IndexAccessors indexAccessors;
	private IndexManager<?> indexManager;
	private String indexName;

	private SessionContext tenant1SessionContext = new StubSessionContext( TENANT_1 );

	private SessionContext tenant2SessionContext = new StubSessionContext( TENANT_2 );

	@Before
	public void setup() {
		setupHelper.withMultiTenancyConfiguration()
				.withIndex(
						"MappedType", "IndexName",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.withMultiTenancy()
				.setup();

		initData();
	}

	@Test
	public void search_only_returns_elements_of_the_selected_tenant() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().match().onField( "string" ).matching( STRING_VALUE_1 )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().match().onField( "string" ).matching( STRING_VALUE_1 )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	public void search_on_nested_object_only_returns_elements_of_the_tenant() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().nested().onObjectField( "nestedObject" ).match().onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().nested().onObjectField( "nestedObject" ).match().onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( STRING_VALUE_1, INTEGER_VALUE_3 ) );
	}

	@Test
	public void delete_only_deletes_elements_of_the_tenant() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( tenant2SessionContext );

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( tenant2SessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().all().end()
				.build();
		assertThat( projectionQuery ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( STRING_VALUE_1, INTEGER_VALUE_3 );
				b.projection( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		worker.delete( referenceProvider( DOCUMENT_ID_1 ) );

		worker.execute().join();

		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_2 );
		projectionQuery = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().all().end()
				.build();
		assertThat( projectionQuery ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );

		query = searchTarget.query( tenant1SessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void update_only_updates_elements_of_the_tenant() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( tenant2SessionContext );

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> checkQuery = searchTarget.query( tenant2SessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( checkQuery )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		worker.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, UPDATED_STRING );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, UPDATED_STRING );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		worker.execute().join();

		// The tenant 2 has been updated properly.

		SearchQuery<List<?>> query = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().match().onField( "string" ).matching( UPDATED_STRING )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( UPDATED_STRING, INTEGER_VALUE_4 ) );

		query = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().nested().onObjectField( "nestedObject" ).match().onField( "nestedObject.string" ).matching( UPDATED_STRING )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( UPDATED_STRING, INTEGER_VALUE_4 ) );

		// The tenant 1 has not been updated.

		query = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().match().onField( "string" ).matching( UPDATED_STRING )
				.build();
		assertThat( query ).hasNoHits();

		query = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().nested().onObjectField( "nestedObject" ).match().onField( "nestedObject.string" ).matching( UPDATED_STRING )
				.build();
		assertThat( query ).hasNoHits();

		query = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().match().onField( "string" ).matching( STRING_VALUE_1 )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( STRING_VALUE_1, INTEGER_VALUE_1 ) );

		query = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().nested().onObjectField( "nestedObject" ).match().onField( "nestedObject.string" ).matching( STRING_VALUE_1 )
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> b.projection( STRING_VALUE_1, INTEGER_VALUE_1 ) );
	}

	@Test
	public void backend_multi_tenancy_disabled_but_indexes_requiring_multi_tenancy_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Index" );
		thrown.expectMessage( "requires multi-tenancy but backend" );
		thrown.expectMessage( "does not support it in its current configuration." );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.withMultiTenancy()
				.setup();

		initData();
	}

	@Test
	public void using_multi_tenancy_for_query_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for the backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_query_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.setup();

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( tenant1SessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void using_multi_tenancy_for_add_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for the backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_add_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.setup();

		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( tenant1SessionContext );

		worker.add( referenceProvider( DOCUMENT_ID_3 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_3 );
			indexAccessors.integer.write( document, INTEGER_VALUE_5 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_3 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_5 );
		} );

		worker.execute().join();
	}

	@Test
	public void using_multi_tenancy_for_update_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for the backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_update_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.setup();

		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( tenant1SessionContext );

		worker.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, UPDATED_STRING );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, UPDATED_STRING );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		worker.execute().join();
	}

	@Test
	public void using_multi_tenancy_for_delete_while_disabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for the backend" );

		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName-using_multi_tenancy_for_delete_while_disabled_throws_exception",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.setup();

		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( tenant1SessionContext );
		worker.delete( referenceProvider( DOCUMENT_ID_1 ) );
		worker.execute().join();
	}

	@Test
	public void not_using_multi_tenancy_for_query_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( new StubSessionContext() )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );
	}

	@Test
	public void not_using_multi_tenancy_for_add_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( new StubSessionContext() );

		worker.add( referenceProvider( DOCUMENT_ID_3 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_3 );
			indexAccessors.integer.write( document, INTEGER_VALUE_5 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_3 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_5 );
		} );

		worker.execute().join();
	}

	@Test
	public void not_using_multi_tenancy_for_update_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( new StubSessionContext() );

		worker.update( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, UPDATED_STRING );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, UPDATED_STRING );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		worker.execute().join();
	}

	@Test
	public void not_using_multi_tenancy_for_delete_while_enabled_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Backend" );
		thrown.expectMessage( "has multi-tenancy enabled, but no tenant identifier is provided." );

		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( new StubSessionContext() );
		worker.delete( referenceProvider( DOCUMENT_ID_1 ) );
		worker.execute().join();
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( tenant1SessionContext );
		worker.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_1 );
			indexAccessors.integer.write( document, INTEGER_VALUE_1 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_1 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_1 );
		} );

		worker.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_2 );
			indexAccessors.integer.write( document, INTEGER_VALUE_2 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_2 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_2 );
		} );

		worker.execute().join();

		worker = indexManager.createWorker( tenant2SessionContext );
		worker.add( referenceProvider( DOCUMENT_ID_1 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_1 );
			indexAccessors.integer.write( document, INTEGER_VALUE_3 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_1 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_3 );
		} );

		worker.add( referenceProvider( DOCUMENT_ID_2 ), document -> {
			indexAccessors.string.write( document, STRING_VALUE_2 );
			indexAccessors.integer.write( document, INTEGER_VALUE_4 );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, STRING_VALUE_2 );
			indexAccessors.nestedObject.integer.write( nestedObject, INTEGER_VALUE_4 );
		} );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( tenant1SessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		SearchQuery<List<?>> projectionQuery = searchTarget.query( tenant1SessionContext )
				.asProjections( "string", "integer" )
				.predicate().all().end()
				.build();
		assertThat( projectionQuery ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( STRING_VALUE_1, INTEGER_VALUE_1 );
				b.projection( STRING_VALUE_2, INTEGER_VALUE_2 );
		} );

		query = searchTarget.query( tenant2SessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, DOCUMENT_ID_1, DOCUMENT_ID_2 );

		projectionQuery = searchTarget.query( tenant2SessionContext )
				.asProjections( "string", "integer" )
				.predicate().all().end()
				.build();
		assertThat( projectionQuery ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( STRING_VALUE_1, INTEGER_VALUE_3 );
				b.projection( STRING_VALUE_2, INTEGER_VALUE_4 );
		} );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<Integer> integer;
		final ObjectAccessors nestedObject;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().store( Store.YES ).createAccessor();
			integer = root.field( "integer" ).asInteger().store( Store.YES ).createAccessor();
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
			string = objectField.field( "string" ).asString().store( Store.YES ).createAccessor();
			integer = objectField.field( "integer" ).asInteger().store( Store.YES ).createAccessor();
		}
	}
}
