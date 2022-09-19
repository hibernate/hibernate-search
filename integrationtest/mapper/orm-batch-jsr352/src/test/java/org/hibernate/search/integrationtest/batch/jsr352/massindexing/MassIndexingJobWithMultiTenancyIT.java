/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil.JOB_TIMEOUT_MS;
import static org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil.findIndexedResultsInTenant;

import java.util.Arrays;
import java.util.List;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;

import org.hibernate.SessionFactory;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.util.BackendConfigurations;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * @author Mincong Huang
 */
public class MassIndexingJobWithMultiTenancyIT {

	private static final String TARGET_TENANT_ID = "targetTenant";

	private static final String UNUSED_TENANT_ID = "unusedTenant";

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private JobOperator jobOperator;

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( Company.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.withBackendProperty( "multi_tenancy.strategy", "discriminator" )
				.tenants( TARGET_TENANT_ID, UNUSED_TENANT_ID );
	}

	@Before
	public void initData() {
		jobOperator = JobTestUtil.getAndCheckRuntime();

		setupHolder.with( TARGET_TENANT_ID )
				.runInTransaction( session -> companies.forEach( session::persist ) );
		setupHolder.with( TARGET_TENANT_ID )
				.runNoTransaction( session -> Search.session( session ).workspace( Company.class ).purge() );
	}

	@Test
	public void shouldHandleTenantIds() throws Exception {
		SessionFactory sessionFactory = setupHolder.sessionFactory();

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

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", TARGET_TENANT_ID ) )
				.hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", TARGET_TENANT_ID ) )
				.hasSize( 1 );
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", TARGET_TENANT_ID ) )
				.hasSize( 1 );

		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Google", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Red Hat", UNUSED_TENANT_ID ) )
				.isEmpty();
		assertThat( findIndexedResultsInTenant( sessionFactory, Company.class, "name", "Microsoft", UNUSED_TENANT_ID ) )
				.isEmpty();
	}
}
