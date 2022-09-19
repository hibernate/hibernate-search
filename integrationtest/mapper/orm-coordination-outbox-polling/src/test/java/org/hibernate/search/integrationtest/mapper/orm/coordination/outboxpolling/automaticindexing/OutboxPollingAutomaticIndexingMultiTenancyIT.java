/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.OutboxPollingTestUtils.awaitAllAgentsRunningInOneCluster;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.PerSessionFactoryIndexingCountHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.data.Percentage;

/**
 * Tests automatic indexing with multi-tenancy for advanced use cases
 * with configuration specific to the outbox-polling coordination strategy.
 */
@TestForIssue(jiraKey = "HSEARCH-4316")
public class OutboxPollingAutomaticIndexingMultiTenancyIT {

	private static final String TENANT_1_ID = "tenant1";
	private static final String TENANT_2_ID = "tenant2";
	private static final String TENANT_3_ID = "tenant3";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private final PerSessionFactoryIndexingCountHelper indexingCountHelper =
			new PerSessionFactoryIndexingCountHelper( counters );

	private void setup(String hbm2ddlAction, UnaryOperator<OrmSetupHelper.SetupContext> config) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.with( indexingCountHelper::expectSchema ) );

		OrmSetupHelper.SetupContext context = setupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.tenants( TENANT_1_ID, TENANT_2_ID, TENANT_3_ID )
				.with( indexingCountHelper::bind )
				.with( config );

		context.setup( IndexedEntity.class );
	}

	@Test
	public void perTenantEventProcessorConfiguration() {
		// Session factory 0 is configured to handle all tenants (which is the default)
		setup( "create-drop", c -> c );
		// Session factory 1 is configured to handle tenant 1 only
		setup( "none", c -> c
				.withProperty( "hibernate.search.coordination.event_processor.enabled", false )
				.withProperty( "hibernate.search.coordination.tenants." + TENANT_1_ID + ".event_processor.enabled", true ) );
		// Session factory 2 is configured to handle tenants 2 and 3 only
		setup( "none", c -> c
				.withProperty( "hibernate.search.coordination.event_processor.enabled", false )
				.withProperty( "hibernate.search.coordination.tenants." + TENANT_2_ID + ".event_processor.enabled", true )
				.withProperty( "hibernate.search.coordination.tenants." + TENANT_3_ID + ".event_processor.enabled", true ) );

		backendMock.verifyExpectationsMet();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );
		awaitAllAgentsRunningInOneCluster( with( sessionFactory, TENANT_1_ID ), 2 );
		awaitAllAgentsRunningInOneCluster( with( sessionFactory, TENANT_2_ID ), 2 );
		awaitAllAgentsRunningInOneCluster( with( sessionFactory, TENANT_3_ID ), 2 );

		SessionFactory sessionFactoryForAllTenants = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 500;
		for ( String tenantId : new String[] { TENANT_1_ID, TENANT_2_ID, TENANT_3_ID } ) {
			with( sessionFactoryForAllTenants, tenantId ).runInTransaction( session -> {
				for ( int i = 0; i < entityCount; i++ ) {
					IndexedEntity entity = new IndexedEntity( i, "initial" );
					session.persist( entity );

					backendMock.expectWorks( IndexedEntity.NAME, tenantId )
							.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
				}
			} );
		}
		backendMock.verifyExpectationsMet();

		// All works must be executed exactly once
		indexingCountHelper.indexingCounts( TENANT_1_ID ).assertAcrossAllSessionFactories().isEqualTo( entityCount );
		indexingCountHelper.indexingCounts( TENANT_2_ID ).assertAcrossAllSessionFactories().isEqualTo( entityCount );
		indexingCountHelper.indexingCounts( TENANT_3_ID ).assertAcrossAllSessionFactories().isEqualTo( entityCount );

		// The workload must be spread uniformly across factories handling each tenant (with some tolerance)
		// Tenant 2 is handled by session factories 0 and 1 only
		indexingCountHelper.indexingCounts( TENANT_1_ID ).assertForSessionFactory( 0 )
				.isCloseTo( entityCount / 2, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts( TENANT_1_ID ).assertForSessionFactory( 1 )
				.isCloseTo( entityCount / 2, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts( TENANT_1_ID ).assertForSessionFactory( 2 )
				.isZero();
		// Tenant 2 is handled by session factories 0 and 2 only
		indexingCountHelper.indexingCounts( TENANT_2_ID ).assertForSessionFactory( 0 )
				.isCloseTo( entityCount / 2, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts( TENANT_2_ID ).assertForSessionFactory( 1 )
				.isZero();
		indexingCountHelper.indexingCounts( TENANT_2_ID ).assertForSessionFactory( 2 )
				.isCloseTo( entityCount / 2, Percentage.withPercentage( 25 ) );
		// Tenant 3 is handled by session factories 0 and 2 only
		indexingCountHelper.indexingCounts( TENANT_3_ID ).assertForSessionFactory( 0 )
				.isCloseTo( entityCount / 2, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts( TENANT_3_ID ).assertForSessionFactory( 1 )
				.isZero();
		indexingCountHelper.indexingCounts( TENANT_3_ID ).assertForSessionFactory( 2 )
				.isCloseTo( entityCount / 2, Percentage.withPercentage( 25 ) );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		static volatile AtomicReference<Runnable> getTextConcurrentOperation = new AtomicReference<>( () -> {} );

		private Integer id;
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@FullTextField
		public String getText() {
			getTextConcurrentOperation.get().run();
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
