/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.logging.impl.TestLog.TEST_LOGGER;

import java.util.List;
import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;

/**
 * @author Yoann Rodiere
 */
public final class JobTestUtil {

	public static final int JOB_TIMEOUT_MS = 30_000;

	private static final int THREAD_SLEEP_MS = 100;
	private static final String JAKARTA_BATCH_TYPE_FOR_IDE_TESTS = "jbatch";

	private static volatile JobOperator operator;

	private JobTestUtil() {
	}

	public static JobOperator getOperator() {
		if ( operator == null ) {
			synchronized (JobTestUtil.class) {
				if ( operator == null ) {
					operator = createAndCheckOperator();
				}
			}
		}

		return operator;
	}

	private static JobOperator createAndCheckOperator() {
		JobOperator operator = BatchRuntime.getJobOperator();
		String expectedType = System.getProperty( "org.hibernate.search.integrationtest.jakarta.batch.type" );

		// only for tests run from the IDE only
		if ( expectedType == null ) {
			expectedType = JAKARTA_BATCH_TYPE_FOR_IDE_TESTS;
		}

		assertThat( operator ).extracting( Object::getClass ).asString()
				.contains( expectedType );
		TEST_LOGGER.infof( "Jakarta Batch operator type is %s (%s)", expectedType, operator.getClass() );
		return operator;
	}

	public static JobExecution startJobAndWaitForSuccessNoRetry(Properties jobParams) throws InterruptedException {
		JobOperator jobOperator = getOperator();
		long execId = jobOperator.start( MassIndexingJob.NAME, jobParams );
		JobExecution jobExec = jobOperator.getJobExecution( execId );
		jobExec = JobTestUtil.waitForTermination( jobExec );
		assertThat( jobExec.getBatchStatus() )
				.as( "Status of job " + jobExec.getJobName() )
				.isEqualTo( BatchStatus.COMPLETED );
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( jobExec.getExecutionId() );
		for ( StepExecution stepExecution : stepExecutions ) {
			assertThat( stepExecution.getBatchStatus() )
					.as( "Status of step " + stepExecution.getStepName() )
					.isEqualTo( BatchStatus.COMPLETED );
		}
		return jobExec;
	}

	public static JobExecution waitForTermination(JobExecution jobExecution)
			throws InterruptedException {
		long endTime = System.nanoTime() + JOB_TIMEOUT_MS * 1_000_000L;

		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& System.nanoTime() < endTime ) {

			long executionId = jobExecution.getExecutionId();
			TEST_LOGGER.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					THREAD_SLEEP_MS
			);
			Thread.sleep( THREAD_SLEEP_MS );
			jobExecution = getOperator().getJobExecution( executionId );
		}

		return jobExecution;
	}

	public static <T> int nbDocumentsInIndex(EntityManagerFactory emf, Class<T> clazz) {
		return with( emf ).applyNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			searchSession.workspace().refresh();
			long totalHitCount = searchSession.search( clazz ).where( f -> f.matchAll() ).fetchTotalHitCount();
			return Math.toIntExact( totalHitCount );
		} );
	}

	public static <T> List<T> findIndexedResults(EntityManagerFactory emf, Class<T> clazz, String key, String value) {
		SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
		return with( sessionFactory )
				.applyNoTransaction( session -> find( session, clazz, key, value ) );
	}

	public static <T> List<T> findIndexedResultsInTenant(EntityManagerFactory emf, Class<T> clazz, String key, String value,
			Object tenantId) {
		SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
		return with( sessionFactory, tenantId )
				.applyNoTransaction( session -> find( session, clazz, key, value ) );
	}

	private static <T> List<T> find(Session session, Class<T> clazz, String key, String value) {
		SearchSession searchSession = Search.session( session );
		searchSession.workspace().refresh();
		return searchSession.search( clazz )
				.where( f -> f.match().field( key ).matching( value ) )
				.fetchHits( 1000 );
	}

	public static EntityTypeDescriptor<?, ?> createEntityTypeDescriptor(EntityManagerFactory emf, Class<?> clazz) {
		SearchMapping mapping = Search.mapping( emf );
		BatchMappingContext mappingContext = (BatchMappingContext) mapping;
		HibernateOrmLoadingTypeContext<?> type = mappingContext.typeContextProvider()
				.byEntityName().getOrFail( mapping.indexedEntity( clazz ).jpaName() );
		return EntityTypeDescriptor.create( emf.unwrap( SessionFactoryImplementor.class ), type );
	}
}
