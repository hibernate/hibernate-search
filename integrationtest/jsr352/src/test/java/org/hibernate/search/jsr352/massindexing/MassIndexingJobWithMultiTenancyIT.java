/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.jsr352.test.util.JobTestUtil.findIndexedResultsInTenant;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Mincong Huang
 */
@RequiresDialect(
		comment = "The connection provider for this test ignores configuration and requires H2",
		strictMatching = true,
		value = org.hibernate.dialect.H2Dialect.class
)
// This test uses native APIs to load configuration, but ES-related configuration is in JPA's persistence.xml
@Category(SkipOnElasticsearch.class)
public class MassIndexingJobWithMultiTenancyIT extends SearchTestBase {

	private static final String TARGET_TENANT_ID = "targetTenant";

	private static final String UNUSED_TENANT_ID = "unusedTenant";

	private static final int JOB_TIMEOUT_MS = 10_000;

	private JobOperator jobOperator = BatchRuntime.getJobOperator();

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		persist( TARGET_TENANT_ID, companies );
		purgeAll( TARGET_TENANT_ID, Company.class );
	}

	private <T> void persist(String tenantId, List<T> entities) {
		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			session.getTransaction().begin();
			entities.forEach( session::persist );
			session.getTransaction().commit();
		}
	}

	private void purgeAll(String tenantId, Class<?> entityType) throws IOException {
		FullTextSession session = Search.getFullTextSession( openSessionWithTenantId( tenantId ) );
		session.purgeAll( entityType );
		session.flushToIndexes();
		session.close();
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

		EntityManagerFactory emf = getSessionFactory().unwrap( EntityManagerFactory.class );

		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Google", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Red Hat", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Microsoft", TARGET_TENANT_ID ) ).hasSize( 1 );

		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Google", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Red Hat", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Microsoft", UNUSED_TENANT_ID ) ).isEmpty();
	}

	private Session openSessionWithTenantId(String tenantId) {
		return getSessionFactory().withOptions().tenantIdentifier( tenantId ).openSession();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Company.class };
	}

	@Override
	public Set<String> multiTenantIds() {
		return CollectionHelper.asSet( TARGET_TENANT_ID, UNUSED_TENANT_ID );
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( "hibernate.search.indexing_strategy", "manual" );
	}

}
