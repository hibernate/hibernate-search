/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.util;

import java.util.List;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

/**
 * @author Yoann Rodiere
 */
public final class JobTestUtil {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final int THREAD_SLEEP = 1000;

	private JobTestUtil() {
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

	public static <T> List<T> findIndexedResults(EntityManagerFactory emf, Class<T> clazz, String key, String value) {
		SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
		try (Session session = sessionFactory.openSession()) {
			return find( session, clazz, key, value );
		}
	}

	public static <T> List<T> findIndexedResultsInTenant(EntityManagerFactory emf, Class<T> clazz, String key, String value, String tenantId) {
		SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
		try (Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession()) {
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

}
