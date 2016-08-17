/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Mincong Huang
 */
@RunWith(org.jboss.byteman.contrib.bmunit.BMUnitRunner.class)
@BMRules(rules = {
		@BMRule(
				name = "Create count-down before the step partitioning",
				targetClass = "org.hibernate.search.jsr352.internal.steps.lucene.PartitionMapper",
				targetMethod = "mapPartitions",
				targetLocation = "AT EXIT",
				action = "createCountDown(\"beforeRestart\", 100)"
		),
		@BMRule(
				name = "Count down for each item read, interrupt the job when counter is 0",
				targetClass = "org.hibernate.search.jsr352.internal.steps.lucene.EntityReader",
				targetMethod = "readItem",
				targetLocation = "AT ENTRY",
				condition = "countDown(\"beforeRestart\")",
				action = "throw new java.lang.InterruptedException(\"Job is interrupted by Byteman.\")"
		)
})
public class RestartChunkIT {

	private static final Logger LOGGER = Logger.getLogger( RestartChunkIT.class );

	private final long DB_COMP_ROWS = 100;
	private final long DB_PERS_ROWS = 50;

	private JobOperator jobOperator;
	private EntityManagerFactory emf;

	@Before
	public void setup() {

		String[][] str = new String[][]{
				{ "Google", "Sundar", "Pichai" },
				{ "Red Hat", "James", "M. Whitehurst" },
				{ "Microsoft", "Satya", "Nadella" },
				{ "Facebook", "Mark", "Zuckerberg" },
				{ "Amazon", "Jeff", "Bezos" }
		};

		jobOperator = JobFactory.getJobOperator();
		emf = Persistence.createEntityManagerFactory( "h2" );

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			em.persist( new Company( str[i % 5][0] ) );
		}
		for ( int i = 0; i < DB_PERS_ROWS; i++ ) {
			String firstName = str[i % 5][1];
			String lastName = str[i % 5][2];
			String id = String.format( "%2d%c", i, firstName.charAt( 0 ) );
			em.persist( new Person( id, firstName, lastName ) );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testJob() throws InterruptedException {

		List<Company> companies = findClasses( Company.class, "name", "Google" );
		List<Person> people = findClasses( Person.class, "firstName", "Sundar" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );

		// start the job
		MassIndexer massIndexer = new MassIndexer()
				.isJavaSE( true )
				.addRootEntities( Company.class, Person.class )
				.entityManagerFactory( emf )
				.jobOperator( jobOperator );
		long execId1 = massIndexer.start();
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		jobExec1 = keepTestAlive( jobExec1 );
		// job will be stopped by the byteman
		for ( StepExecution stepExec : jobOperator.getStepExecutions( execId1 ) ) {
			if ( stepExec.getStepName().equals( "produceLuceneDoc" ) ) {
				assertEquals( BatchStatus.FAILED, stepExec.getBatchStatus() );
			}
		}

		// restart the job
		long execId2 = massIndexer.restart();
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = keepTestAlive( jobExec2 );
		for ( StepExecution stepExec : jobOperator.getStepExecutions( execId2 ) ) {
			assertEquals( BatchStatus.COMPLETED, stepExec.getBatchStatus() );
		}

		// search again
		companies = findClasses( Company.class, "name", "google" );
		people = findClasses( Person.class, "firstName", "Sundar" );
		assertEquals( DB_COMP_ROWS / 5, companies.size() );
		assertEquals( DB_PERS_ROWS / 5, people.size() );
	}

	private <T> List<T> findClasses(Class<T> clazz, String key, String value) {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( clazz ).get()
				.keyword().onField( key ).matching( value )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<T> result = ftem.createFullTextQuery( luceneQuery ).getResultList();
		em.close();
		return result;
	}

	private JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
		int tries = 0;
		while ( ( jobExecution.getBatchStatus().equals( BatchStatus.STARTING )
				|| jobExecution.getBatchStatus().equals( BatchStatus.STARTED ) )
				&& tries < 10 ) {

			long executionId = jobExecution.getExecutionId();
			LOGGER.infof( "Job (id=%d) %s, thread sleep 1000 ms...",
					executionId, jobExecution.getBatchStatus() );
			Thread.sleep( 1000 );
			jobExecution = jobOperator.getJobExecution( executionId );
			tries++;
		}
		return jobExecution;
	}

	@After
	public void shutdownJPA() {
		emf.close();
	}
}
