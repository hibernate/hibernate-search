/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
import org.hibernate.search.jsr352.massindexing.BatchIndexingJob;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.massindexing.test.entity.Person;
import org.hibernate.search.jsr352.test.util.JobFactory;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
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
				targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.PartitionMapper",
				targetMethod = "mapPartitions",
				targetLocation = "AT EXIT",
				action = "createCountDown(\"beforeRestart\", 100)"
		),
		@BMRule(
				name = "Count down for each item read, interrupt the job when counter is 0",
				targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader",
				targetMethod = "readItem",
				targetLocation = "AT ENTRY",
				condition = "countDown(\"beforeRestart\")",
				action = "throw new java.lang.InterruptedException(\"Job is interrupted by Byteman.\")"
		)
})
public class RestartChunkIT {

	private static final String PERSISTENCE_UNIT_NAME = "h2";

	private static final int JOB_TIMEOUT_MS = 10_000;

	private static final long DB_COMP_ROWS = 100;
	private static final long DB_PERS_ROWS = 50;

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
		emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			em.persist( new Company( str[i % 5][0] ) );
		}
		for ( int i = 0; i < DB_PERS_ROWS; i++ ) {
			String firstName = str[i % 5][1];
			String lastName = str[i % 5][2];
			String id = String.format( Locale.ROOT, "%2d%c", i, firstName.charAt( 0 ) );
			em.persist( new Person( id, firstName, lastName ) );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testJob() throws InterruptedException, IOException {

		List<Company> companies = findClasses( Company.class, "name", "Google" );
		List<Person> people = findClasses( Person.class, "firstName", "Sundar" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );

		// start the job
		long execId1 = BatchIndexingJob.forEntities( Company.class, Person.class )
				.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
				.underJavaSE( jobOperator )
				.checkpointFreq( 10 )
				.start();
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		jobExec1 = JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );
		// job will be stopped by the byteman
		for ( StepExecution stepExec : jobOperator.getStepExecutions( execId1 ) ) {
			if ( stepExec.getStepName().equals( "produceLuceneDoc" ) ) {
				assertEquals( BatchStatus.FAILED, stepExec.getBatchStatus() );
			}
		}

		// restart the job
		long execId2 = BatchIndexingJob.restart( execId1, jobOperator );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = JobTestUtil.waitForTermination( jobOperator, jobExec2, JOB_TIMEOUT_MS );
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

	@After
	public void shutdownJPA() {
		emf.close();
	}
}
