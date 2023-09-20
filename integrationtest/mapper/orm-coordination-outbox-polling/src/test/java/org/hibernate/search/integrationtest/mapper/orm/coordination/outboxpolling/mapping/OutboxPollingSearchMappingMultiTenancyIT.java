/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.OutboxPollingExtension;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OutboxPollingSearchMappingMultiTenancyIT {
	private static final String TENANT_1_ID = "tenant1";
	private static final String TENANT_2_ID = "tenant2";
	private static final String TENANT_3_ID = "tenant3";

	@Parameterized.Parameters(name = "entity = {0}")
	public static Object[][] params() {
		return new Object[][] {
				{
						IndexedMultiTenantEntity.INDEX,
						IndexedMultiTenantEntity.class,
						(BiConsumer<Session, Integer>) (session, id) -> {
							IndexedMultiTenantEntity entity1 = new IndexedMultiTenantEntity();
							entity1.setId( id );
							entity1.setIndexedField( "initialValue" );
							session.persist( entity1 );
						},
						(Consumer<OrmSetupHelper.SetupContext>) context -> context.tenants( false, TENANT_1_ID, TENANT_2_ID,
								TENANT_3_ID )
				},
				{
						IndexedEntity.INDEX,
						IndexedEntity.class,
						(BiConsumer<Session, Integer>) (session, id) -> {
							IndexedEntity entity1 = new IndexedEntity();
							entity1.setId( id );
							entity1.setIndexedField( "initialValue" );
							session.persist( entity1 );
						},
						(Consumer<OrmSetupHelper.SetupContext>) context -> {
							context.tenants( TENANT_1_ID, TENANT_2_ID, TENANT_3_ID );
						}
				},
		};
	}

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;
	private OutboxPollingSearchMapping searchMapping;
	private AbortedEventsGenerator abortedEventsGenerator1;
	private AbortedEventsGenerator abortedEventsGenerator2;

	@Parameterized.Parameter(0)
	public String indexName;

	@Parameterized.Parameter(1)
	public Class<?> indexedEntity;

	@Parameterized.Parameter(2)
	public BiConsumer<Session, Integer> entityCreator;

	@Parameterized.Parameter(3)
	public Consumer<OrmSetupHelper.SetupContext> tenantConfigurer;

	@Before
	public void before() {
		backendMock.expectSchema( indexName, b -> b.field( "indexedField", String.class ) );
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start();
		tenantConfigurer.accept( setupContext );
		sessionFactory = setupContext
				.withProperty( "hibernate.search.coordination.event_processor.retry_delay", 0 )
				.setup( indexedEntity );
		backendMock.verifyExpectationsMet();
		searchMapping = Search.mapping( sessionFactory ).extension( OutboxPollingExtension.get() );
		abortedEventsGenerator1 =
				new AbortedEventsGenerator( sessionFactory, backendMock, TENANT_1_ID, indexName, entityCreator );
		abortedEventsGenerator2 =
				new AbortedEventsGenerator( sessionFactory, backendMock, TENANT_2_ID, indexName, entityCreator );
	}

	@Test
	public void countAbortedEvents_noTenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.countAbortedEvents() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is enabled but no tenant id is specified.",
						"Available tenants are: '[tenant1, tenant2, tenant3]'."
				);
	}

	@Test
	public void countAbortedEvents_wrongTenantId() {
		assertThatThrownBy( () -> searchMapping.countAbortedEvents( "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot target tenant 'tenantX'",
						"Currently configured tenant identifiers: [tenant1, tenant2, tenant3]."
				);
	}

	@Test
	public void reprocessAbortedEvents_noTenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.reprocessAbortedEvents() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is enabled but no tenant id is specified.",
						"Available tenants are: '[tenant1, tenant2, tenant3]'."
				);
	}

	@Test
	public void reprocessAbortedEvents_wrongTenantId() {
		assertThatThrownBy( () -> searchMapping.reprocessAbortedEvents( "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot target tenant 'tenantX'",
						"Currently configured tenant identifiers: [tenant1, tenant2, tenant3]."
				);
	}

	@Test
	public void clearAllAbortedEvents_noTenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.clearAllAbortedEvents() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is enabled but no tenant id is specified.",
						"Available tenants are: '[tenant1, tenant2, tenant3]'."
				);
	}

	@Test
	public void clearAllAbortedEvents_wrongTenantId() {
		assertThatThrownBy( () -> searchMapping.clearAllAbortedEvents( "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot target tenant 'tenantX'",
						"Currently configured tenant identifiers: [tenant1, tenant2, tenant3]."
				);
	}

	@Test
	public void clearAllAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		abortedEventsGenerator1.generateThreeAbortedEvents();
		abortedEventsGenerator2.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		assertThat( searchMapping.clearAllAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();
	}

	@Test
	public void reprocessAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		abortedEventsGenerator1.generateThreeAbortedEvents();
		List<Integer> generatedIds = abortedEventsGenerator2.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		backendMock.expectWorks( indexName, TENANT_2_ID )
				.createAndExecuteFollowingWorks()
				.addOrUpdate( Integer.toString( generatedIds.get( 0 ) ), b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( Integer.toString( generatedIds.get( 1 ) ), b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( Integer.toString( generatedIds.get( 2 ) ), b -> b
						.field( "indexedField", "initialValue" )
				);
		assertThat( searchMapping.reprocessAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );
		backendMock.verifyExpectationsMet();

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();
	}
}
