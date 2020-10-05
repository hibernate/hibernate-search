/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.jsr352.test.util.JobTestUtil.findIndexedResultsInTenant;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.jsr352.test.util.MultitenancyTestHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.util.Version;

/**
 * @author Mincong Huang
 */
// This test uses native APIs to load configuration, but ES-related configuration is in JPA's persistence.xml
@Category(SkipOnElasticsearch.class)
public class MassIndexingJobWithMultiTenancyIT {

	private static final String TARGET_TENANT_ID = "targetTenant";

	private static final String UNUSED_TENANT_ID = "unusedTenant";

	private static final int JOB_TIMEOUT_MS = 10_000;

	private MultitenancyTestHelper multitenancyTestHelper = new MultitenancyTestHelper(
			CollectionHelper.asSet( TARGET_TENANT_ID, UNUSED_TENANT_ID ) );

	private SessionFactory sessionFactory;

	private JobOperator jobOperator = BatchRuntime.getJobOperator();

	private final List<Company> companies = Arrays.asList(
			new Company( "Google" ),
			new Company( "Red Hat" ),
			new Company( "Microsoft" )
	);

	@Before
	public void setUp() throws Exception {
		assumeTrue( "The connection provider for this test ignores configuration and requires H2",
				Dialect.getDialect() instanceof org.hibernate.dialect.H2Dialect );

		sessionFactory = buildSessionFactory();
		persist( TARGET_TENANT_ID, companies );
		purgeAll( TARGET_TENANT_ID, Company.class );
	}

	@After
	public void cleanUp() {
		try {
			sessionFactory.close();
		}
		finally {
			multitenancyTestHelper.close();
		}
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

		EntityManagerFactory emf = sessionFactory.unwrap( EntityManagerFactory.class );

		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Google", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Red Hat", TARGET_TENANT_ID ) ).hasSize( 1 );
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Microsoft", TARGET_TENANT_ID ) ).hasSize( 1 );

		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Google", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Red Hat", UNUSED_TENANT_ID ) ).isEmpty();
		assertThat( findIndexedResultsInTenant( emf, Company.class, "name", "Microsoft", UNUSED_TENANT_ID ) ).isEmpty();
	}

	private Session openSessionWithTenantId(String tenantId) {
		return sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession();
	}

	private Map<String, Object> getConfigurationSettings() {
		Map<String, Object> configurationSettings = new HashMap<>();
		configurationSettings.put( "hibernate.search.lucene_version", Version.LATEST.toString() );
		configurationSettings.put( "hibernate.search.default.directory_provider", "local-heap" );
		configurationSettings.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		configurationSettings.put( "hibernate.search.default.indexwriter.merge_factor", "100" );
		configurationSettings.put( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
		configurationSettings.put( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );

		configurationSettings.put( "hibernate.search.indexing_strategy", "manual" );

		return configurationSettings;
	}

	private SessionFactoryImplementor buildSessionFactory() {
		Map<String, Object> settings = getConfigurationSettings();
		multitenancyTestHelper.forceConfigurationSettings( settings );

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySettings( settings );

		multitenancyTestHelper.enableIfNeeded( registryBuilder );

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry );
		ms.addAnnotatedClass( Company.class );

		Metadata metadata = ms.buildMetadata();
		multitenancyTestHelper.exportSchema( serviceRegistry, metadata, settings );

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		return (SessionFactoryImplementor) sfb.build();
	}

}
