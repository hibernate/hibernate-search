/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.batch.jsr352.core.massindexing.test.util.JobTestUtil.findIndexedResultsInTenant;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.batch.jsr352.core.massindexing.test.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.mapper.orm.Search;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
@RequiresDialect(
		comment = "The connection provider for this test ignores configuration and requires H2",
		strictMatching = true,
		value = org.hibernate.dialect.H2Dialect.class
)
public class MassIndexingJobWithMultiTenancyIT {

	private static final String TARGET_TENANT_ID = "targetTenant";

	private static final String UNUSED_TENANT_ID = "unusedTenant";

	private static final int JOB_TIMEOUT_MS = 10_000;

	protected EntityManagerFactory emf;

	private JobOperator jobOperator = BatchRuntime.getJobOperator();

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);

	@Before
	public void setUp() throws Exception {
		emf = Persistence.createEntityManagerFactory( "lucene_multiTenancy_pu" );
		persist( TARGET_TENANT_ID, companies );
		purgeAll( TARGET_TENANT_ID, Company.class );
	}

	public void tearDown() {
		if ( emf != null ) {
			emf.close();
		}
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

		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Google", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Red Hat", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Microsoft", TARGET_TENANT_ID ) ).hasSize( 1 );

		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Google", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Red Hat", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Microsoft", UNUSED_TENANT_ID ) ).isEmpty();
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
		return emf.unwrap( SessionFactory.class ).withOptions().tenantIdentifier( tenantId ).openSession();
	}
}
