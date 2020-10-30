/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil.findIndexedResultsInTenant;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class MassIndexingJobWithMultiTenancyIT {

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration() );

	private static final String TARGET_TENANT_ID = "targetTenant";

	private static final String UNUSED_TENANT_ID = "unusedTenant";

	private static final int JOB_TIMEOUT_MS = 10_000;

	private SessionFactory sessionFactory;

	private JobOperator jobOperator = BatchRuntime.getJobOperator();

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);

	@Before
	public void setUp() throws Exception {
		sessionFactory = ormSetupHelper
				.start()
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						AutomaticIndexingStrategyName.NONE )
				.withBackendProperty( "multi_tenancy.strategy", "discriminator" )
				.tenants( TARGET_TENANT_ID, UNUSED_TENANT_ID )
				.setup( Company.class );

		persist( TARGET_TENANT_ID, companies );
		purgeAll( TARGET_TENANT_ID, Company.class );
	}

	@Test
	public void shouldHandleTenantIds() throws Exception {
		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.tenantId( TARGET_TENANT_ID )
						.build()
		);

		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertThat( jobExecution.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", TARGET_TENANT_ID ) ).hasSize( 1 );

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", UNUSED_TENANT_ID ) ).isEmpty();
	}

	private <T> void persist(String tenantId, List<T> entities) {
		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			session.getTransaction().begin();
			entities.forEach( session::persist );
			session.getTransaction().commit();
		}
	}

	private void purgeAll(String tenantId, Class<?> entityType) throws IOException {
		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			Search.session( session ).workspace( entityType ).purge();
		}
	}

	private Session openSessionWithTenantId(String tenantId) {
		return sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession();
	}

	private static BackendConfiguration backendConfiguration() {
		return ( BackendConfiguration.isElasticsearch() ) ? new ElasticsearchBackendConfiguration() :
				new LuceneBackendConfiguration();
	}
}
