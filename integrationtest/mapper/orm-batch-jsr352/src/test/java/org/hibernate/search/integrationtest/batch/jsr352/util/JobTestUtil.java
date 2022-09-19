/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Properties;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.SingularIdOrder;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class JobTestUtil {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final int JOB_TIMEOUT_MS = 30_000;

	private static final int THREAD_SLEEP_MS = 100;
	private static final String JSR325_TYPE_FOR_IDE_TESTS = "jbatch";

	private JobTestUtil() {
	}

	public static JobOperator getAndCheckRuntime() {
		JobOperator operator = BatchRuntime.getJobOperator();
		String expectedType = System.getProperty( "org.hibernate.search.integrationtest.jsr352.type" );

		// only for tests run from the IDE only
		if ( expectedType == null ) {
			expectedType = JSR325_TYPE_FOR_IDE_TESTS;
		}

		assertThat( operator ).extracting( Object::getClass ).asString()
				.contains( expectedType );
		log.infof( "JSR-352 operator type is %s (%s)", expectedType, operator.getClass() );
		return operator;
	}

	public static void startJobAndWait(String jobName, Properties jobParams, int timeoutInMs) throws InterruptedException {
		JobOperator jobOperator = getAndCheckRuntime();
		long execId = jobOperator.start( jobName, jobParams );
		JobExecution jobExec = jobOperator.getJobExecution( execId );
		jobExec = JobTestUtil.waitForTermination( jobOperator, jobExec, timeoutInMs );
		assertThat( jobExec.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );
	}

	public static JobExecution waitForTermination(JobOperator jobOperator, JobExecution jobExecution, int timeoutInMs)
			throws InterruptedException {
		long endTime = System.nanoTime() + timeoutInMs * 1_000_000L;

		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& System.nanoTime() < endTime ) {

			long executionId = jobExecution.getExecutionId();
			log.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					THREAD_SLEEP_MS
			);
			Thread.sleep( THREAD_SLEEP_MS );
			jobExecution = jobOperator.getJobExecution( executionId );
		}

		return jobExecution;
	}

	public static <T> int nbDocumentsInIndex(EntityManagerFactory emf, Class<T> clazz) {
		try ( Session session = emf.unwrap( SessionFactory.class ).openSession() ) {
			SearchSession searchSession = Search.session( session );
			searchSession.workspace().refresh();
			long totalHitCount = searchSession.search( clazz ).where( f -> f.matchAll() ).fetchTotalHitCount();
			return Math.toIntExact( totalHitCount );
		}
	}

	public static <T> List<T> findIndexedResults(EntityManagerFactory emf, Class<T> clazz, String key, String value) {
		SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
		try ( Session session = sessionFactory.openSession() ) {
			return find( session, clazz, key, value );
		}
	}

	public static <T> List<T> findIndexedResultsInTenant(EntityManagerFactory emf, Class<T> clazz, String key, String value, String tenantId) {
		SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			return find( session, clazz, key, value );
		}
	}

	private static <T> List<T> find(Session session, Class<T> clazz, String key, String value) {
		SearchSession searchSession = Search.session( session );
		searchSession.workspace().refresh();
		return searchSession.search( clazz )
				.where( f -> f.match().field( key ).matching( value ) )
				.fetchHits( 1000 );
	}

	public static EntityTypeDescriptor createSimpleEntityTypeDescriptor(EntityManagerFactory emf, Class<?> clazz) {
		EntityType<?> entityType = emf.getMetamodel().entity( clazz );
		SingularAttribute<?, ?> idAttribute = entityType.getId( entityType.getIdType().getJavaType() );
		return new EntityTypeDescriptor( clazz, new SingularIdOrder( idAttribute.getName() ) );
	}
}
