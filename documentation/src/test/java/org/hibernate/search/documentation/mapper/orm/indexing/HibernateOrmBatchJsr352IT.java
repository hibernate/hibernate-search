/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmBatchJsr352IT {

	private static final int NUMBER_OF_BOOKS = 1000;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;
	private static final int JOB_TIMEOUT_MS = 30_000;
	private static final int THREAD_SLEEP = 1000;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						AutomaticIndexingStrategyName.NONE
				)
				.setup( Book.class, Author.class );
		initData();
	}

	@Test
	public void simple() throws Exception {
		// tag::simple[]
		Properties jobProps = MassIndexingJob
				.parameters()
				.forEntities( Book.class, Author.class )
				.build();

		JobOperator jobOperator = BatchRuntime.getJobOperator();
		long executionId = jobOperator.start( MassIndexingJob.NAME, jobProps );
		// end::simple[]

		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertThat( jobExecution.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			Search.session( entityManager ).workspace().refresh();

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void hql() throws Exception {
		// tag::hql[]
		Properties jobProps = MassIndexingJob
				.parameters()
				.forEntities( Author.class )
				.restrictedBy( "from Author a where a.lastName = 'Smith1'" )
				.build();

		JobOperator jobOperator = BatchRuntime.getJobOperator();
		long executionId = jobOperator.start( MassIndexingJob.NAME, jobProps );
		// end::hql[]

		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertThat( jobExecution.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			Search.session( entityManager ).workspace().refresh();
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS / 2 );
		} );
	}

	private void assertBookCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		assertThat(
				searchSession.search( Book.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private void assertAuthorCount(EntityManager entityManager, int expectedCount) {
		SearchSession searchSession = Search.session( entityManager );
		assertThat(
				searchSession.search( Author.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		)
				.isEqualTo( expectedCount );
	}

	private Book newBook(int id) {
		Book book = new Book();
		book.setId( id );
		book.setTitle( "This is the title of book #" + id );
		return book;
	}

	private Author newAuthor(int id) {
		Author author = new Author();
		author.setId( id );
		author.setFirstName( "John" + id );
		// use the id % 2
		author.setLastName( "Smith" + ( id % 2 ) );
		return author;
	}

	private void initData() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			try {
				int i = 0;
				while ( i < NUMBER_OF_BOOKS ) {
					entityManager.getTransaction().begin();
					int end = Math.min( i + INIT_DATA_TRANSACTION_SIZE, NUMBER_OF_BOOKS );
					for ( ; i < end; ++i ) {
						Author author = newAuthor( i );

						Book book = newBook( i );
						book.setAuthor( author );
						author.getBooks().add( book );

						entityManager.persist( author );
						entityManager.persist( book );
					}
					entityManager.getTransaction().commit();
				}
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
		} );
	}

	private static JobExecution waitForTermination(JobOperator jobOperator, JobExecution jobExecution, int timeoutInMs)
			throws InterruptedException {
		long endTime = System.currentTimeMillis() + timeoutInMs;

		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& System.currentTimeMillis() < endTime ) {

			long executionId = jobExecution.getExecutionId();
			try {
				Thread.sleep( THREAD_SLEEP );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
			jobExecution = jobOperator.getJobExecution( executionId );
		}

		return jobExecution;
	}
}
