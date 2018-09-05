/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.util;

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
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.util.EntityTypeDescriptor;
import org.hibernate.search.jsr352.massindexing.impl.util.SingularIdOrder;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import org.apache.lucene.search.Query;

import static org.assertj.core.api.Assertions.assertThat;

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
		FullTextEntityManager em = org.hibernate.search.jpa.Search.getFullTextEntityManager( emf.createEntityManager() );
		QueryBuilder queryBuilder = em.getSearchFactory().buildQueryBuilder().forEntity( clazz ).get();
		Query allQuery = queryBuilder.all().createQuery();
		FullTextQuery fullTextQuery = em.createFullTextQuery( allQuery, clazz );
		return fullTextQuery.getResultSize();
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
		FullTextSession fts = Search.getFullTextSession( session );
		Query luceneQuery = fts.getSearchFactory().buildQueryBuilder()
				.forEntity( clazz ).get()
				.keyword().onField( key ).matching( value )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<T> result = fts.createFullTextQuery( luceneQuery ).getResultList();
		return result;
	}

	public static EntityTypeDescriptor createSimpleEntityTypeDescriptor(EntityManagerFactory emf, Class<?> clazz) {
		EntityType<?> entityType = emf.getMetamodel().entity( clazz );
		SingularAttribute<?, ?> idAttribute = entityType.getId( entityType.getIdType().getJavaType() );
		return new EntityTypeDescriptor( clazz, new SingularIdOrder( idAttribute.getName() ) );
	}

}
