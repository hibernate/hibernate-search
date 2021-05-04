/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.multitenancy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test that the backend correctly throws exception
 * when the mapper requires multi-tenancy but it is disabled in the backend,
 * and vice-versa.
 */
public class MultiTenancyMismatchIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubBackendSessionContext tenant1SessionContext = new StubBackendSessionContext( "tenant_1" );

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Test
	public void multiTenancyDisabled_requiredByTheMapper() {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( index )
				.withBackendProperty( "multi_tenancy.strategy", "none" )
				.withMultiTenancy()
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid backend configuration",
						"mapping requires multi-tenancy",
						"no multi-tenancy strategy is set",
						"default backend"
				);
	}

	@Test
	public void multiTenancyEnabled_disabledInTheMapper() {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( index )
				.withBackendProperty( "multi_tenancy.strategy", "discriminator" )
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid backend configuration",
						"mapping not requires multi-tenancy",
						"multi-tenancy strategy is set",
						"default backend"
				);
	}

	@Test
	public void using_multi_tenancy_for_query_while_disabled_throws_exception() {
		setupHelper.start().withIndex( index ).setup();

		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query( tenant1SessionContext )
				.where( f -> f.matchAll() )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid tenant identifier: '" + tenant1SessionContext.tenantIdentifier() + "'",
						"The tenant identifier must be null, because multi-tenancy is disabled for this backend."
				);
	}

	@Test
	public void using_multi_tenancy_for_add_while_disabled_throws_exception() {
		setupHelper.start().withIndex( index ).setup();

		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan( tenant1SessionContext );
			plan.addOrUpdate( referenceProvider( "1" ), document -> { } );
			plan.execute().join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid tenant identifier: '" + tenant1SessionContext.tenantIdentifier() + "'",
						"The tenant identifier must be null, because multi-tenancy is disabled for this backend."
				);
	}

	@Test
	public void using_multi_tenancy_for_update_while_disabled_throws_exception() {
		setupHelper.start().withIndex( index ).setup();

		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan( tenant1SessionContext );
			plan.addOrUpdate( referenceProvider( "1" ), document -> { } );
			plan.execute().join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid tenant identifier: '" + tenant1SessionContext.tenantIdentifier() + "'",
						"The tenant identifier must be null, because multi-tenancy is disabled for this backend."
				);
	}

	@Test
	public void using_multi_tenancy_for_delete_while_disabled_throws_exception() {
		setupHelper.start().withIndex( index ).setup();

		assertThatThrownBy( () -> {
			IndexIndexingPlan plan = index.createIndexingPlan( tenant1SessionContext );
			plan.delete( referenceProvider( "1" ) );
			plan.execute().join();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid tenant identifier: '" + tenant1SessionContext.tenantIdentifier() + "'",
						"The tenant identifier must be null, because multi-tenancy is disabled for this backend."
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
		}
	}
}
