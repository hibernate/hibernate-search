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
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

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

	private static final int THREAD_SLEEP = 1000;

	private JobTestUtil() {
	}

	public static void startJobAndWait(String jobName, Properties jobParams, int timeoutInMs) throws InterruptedException {
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		long execId = jobOperator.start( jobName, jobParams );
		JobExecution jobExec = jobOperator.getJobExecution( execId );
		jobExec = JobTestUtil.waitForTermination( jobOperator, jobExec, timeoutInMs );
		assertThat( jobExec.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );
	}

	public static JobExecution waitForTermination(JobOperator jobOperator, JobExecution jobExecution, int timeoutInMs)
			throws InterruptedException {
		long endTime = System.currentTimeMillis() + timeoutInMs;

		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& System.currentTimeMillis() < endTime ) {

			long executionId = jobExecution.getExecutionId();
			log.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					THREAD_SLEEP );
			Thread.sleep( THREAD_SLEEP );
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
