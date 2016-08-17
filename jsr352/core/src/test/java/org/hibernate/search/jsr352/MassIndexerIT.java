/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class MassIndexerIT {

	private static final Logger LOGGER = Logger.getLogger( MassIndexerIT.class );

	// example dataset
	private final long DB_COMP_ROWS = 3;
	private final long DB_PERS_ROWS = 3;

	private EntityManagerFactory emf;
	private JobOperator jobOperator;

	@Before
	public void setup() {

		jobOperator = JobFactory.getJobOperator();

		List<Company> companies = Arrays.asList(
				new Company( "Google" ),
				new Company( "Red Hat" ),
				new Company( "Microsoft" ) );
		List<Person> people = Arrays.asList(
				new Person( "BG", "Bill", "Gates" ),
				new Person( "LT", "Linus", "Torvalds" ),
				new Person( "SJ", "Steven", "Jobs" ) );
		EntityManager em = null;

		try {
			emf = Persistence.createEntityManagerFactory( "h2" );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			for ( Company c : companies ) {
				em.persist( c );
			}
			for ( Person p : people ) {
				em.persist( p );
			}
			em.getTransaction().commit();
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}
	}

	@Test
	public void testMassIndexer() throws InterruptedException {

		// searches before mass index,
		// expected no results for each search
		List<Company> companies = findClass( Company.class, "name", "Google" );
		List<Person> people = findClass( Person.class, "firstName", "Linus" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );

		JobOperator jobOperator = JobFactory.getJobOperator();
		long executionId = new MassIndexer()
				.addRootEntities( Company.class, Person.class )
				.jobOperator( jobOperator )
				.isJavaSE( true )
				.entityManagerFactory( emf )
				.start();
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = keepTestAlive( jobExecution );
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		for ( StepExecution stepExecution : stepExecutions ) {
			LOGGER.infof( "step %s executed.", stepExecution.getStepName() );
			testBatchStatus( stepExecution );
		}

		companies = findClass( Company.class, "name", "Google" );
		people = findClass( Person.class, "firstName", "Linus" );
		assertEquals( 1, companies.size() );
		assertEquals( 1, people.size() );
	}

	private <T> List<T> findClass(Class<T> clazz, String key, String value) {
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

	public JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
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

	private void testBatchStatus(StepExecution stepExecution) {
		BatchStatus batchStatus = stepExecution.getBatchStatus();
		switch ( stepExecution.getStepName() ) {

			case "produceLuceneDoc":
				for ( Metric m : stepExecution.getMetrics() ) {
					if ( m.getType().equals( MetricType.READ_COUNT ) ) {
						assertEquals( DB_COMP_ROWS + DB_PERS_ROWS, m.getValue() );
					}
					else if ( m.getType().equals( MetricType.WRITE_COUNT ) ) {
						assertEquals( DB_COMP_ROWS + DB_PERS_ROWS, m.getValue() );
					}
				}
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			default:
				break;
		}
	}

	/**
	 * Convert the Metric array contained in StepExecution to a key-value map
	 * for easy access to Metric parameters.
	 *
	 * @param metrics a Metric array contained in StepExecution.
	 * @return a map view of the metrics array.
	 */
	public Map<Metric.MetricType, Long> getMetricsMap(Metric[] metrics) {
		Map<Metric.MetricType, Long> metricsMap = new HashMap<>();
		for ( Metric metric : metrics ) {
			metricsMap.put( metric.getType(), metric.getValue() );
		}
		return metricsMap;
	}

	@After
	public void shutdown() {
		emf.close();
	}
}
