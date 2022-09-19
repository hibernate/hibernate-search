/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.documentation.mapper.orm.indexing.HibernateOrmMassIndexerIT.NUMBER_OF_BOOKS;
import static org.hibernate.search.documentation.mapper.orm.indexing.HibernateOrmMassIndexerIT.assertAuthorCount;
import static org.hibernate.search.documentation.mapper.orm.indexing.HibernateOrmMassIndexerIT.assertBookCount;
import static org.hibernate.search.documentation.mapper.orm.indexing.HibernateOrmMassIndexerIT.initData;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Properties;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmBatchJsr352IT {

	private static final int JOB_TIMEOUT_MS = 30_000;
	private static final int THREAD_SLEEP = 1000;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false )
				.setup( Book.class, Author.class );
		initData( entityManagerFactory, HibernateOrmBatchJsr352IT::newAuthor );
	}

	@Test
	public void simple() throws Exception {
		// tag::simple[]
		Properties jobProps = MassIndexingJob.parameters() // <1>
				.forEntities( Book.class, Author.class ) // <2>
				.build();

		JobOperator jobOperator = BatchRuntime.getJobOperator(); // <3>
		long executionId = jobOperator.start( MassIndexingJob.NAME, jobProps ); // <4>
		// end::simple[]

		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertThat( jobExecution.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			Search.session( entityManager ).workspace().refresh();

			assertBookCount( entityManager, NUMBER_OF_BOOKS );
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	public void hql() throws Exception {
		// tag::hql[]
		Properties jobProps = MassIndexingJob.parameters() // <1>
				.forEntities( Author.class ) // <2>
				.restrictedBy( "from Author a where a.lastName = 'Smith1'" ) // <3>
				.build();

		JobOperator jobOperator = BatchRuntime.getJobOperator(); // <4>
		long executionId = jobOperator.start( MassIndexingJob.NAME, jobProps ); // <5>
		// end::hql[]

		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertThat( jobExecution.getBatchStatus() ).isEqualTo( BatchStatus.COMPLETED );

		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			Search.session( entityManager ).workspace().refresh();
			assertAuthorCount( entityManager, NUMBER_OF_BOOKS / 2 );
		} );
	}

	private static Author newAuthor(int id) {
		Author author = new Author();
		author.setId( id );
		author.setFirstName( "John" + id );
		// use the id % 2
		author.setLastName( "Smith" + ( id % 2 ) );
		return author;
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
