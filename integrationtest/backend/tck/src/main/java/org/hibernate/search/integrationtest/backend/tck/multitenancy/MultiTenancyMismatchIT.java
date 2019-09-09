/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.multitenancy;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test that the backend correctly throws exception
 * when the mapper requires multi-tenancy but it is disabled in the backend,
 * and vice-versa.
 */
public class MultiTenancyMismatchIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final StubBackendSessionContext tenant1SessionContext = new StubBackendSessionContext( "tenant_1" );

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void backend_multi_tenancy_disabled_but_indexes_requiring_multi_tenancy_throws_exception() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Index" );
		thrown.expectMessage( "requires multi-tenancy but the backend" );
		thrown.expectMessage( "does not support it in its current configuration." );

		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withMultiTenancy()
				.setup();
	}

	@Test
	public void using_multi_tenancy_for_query_while_disabled_throws_exception() {
		setupHelper.start()
				.withIndex(
						"IndexName-using_multi_tenancy_for_query_while_disabled_throws_exception",
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		StubMappingScope scope = indexManager.createScope();
		scope.query( tenant1SessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();
	}

	@Test
	public void using_multi_tenancy_for_add_while_disabled_throws_exception() {
		setupHelper.start()
				.withIndex(
						"IndexName-using_multi_tenancy_for_add_while_disabled_throws_exception",
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );
		workPlan.update( referenceProvider( "1" ), document -> { } );
		workPlan.execute().join();
	}

	@Test
	public void using_multi_tenancy_for_update_while_disabled_throws_exception() {
		setupHelper.start()
				.withIndex(
						"IndexName-using_multi_tenancy_for_update_while_disabled_throws_exception",
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );
		workPlan.update( referenceProvider( "1" ), document -> { } );
		workPlan.execute().join();
	}

	@Test
	public void using_multi_tenancy_for_delete_while_disabled_throws_exception() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Tenant identifier" );
		thrown.expectMessage( "is provided, but multi-tenancy is disabled for this backend" );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( tenant1SessionContext );
		workPlan.delete( referenceProvider( "1" ) );
		workPlan.execute().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
		}
	}
}
