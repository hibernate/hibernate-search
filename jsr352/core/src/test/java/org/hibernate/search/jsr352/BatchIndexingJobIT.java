/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
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
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.hibernate.search.jsr352.entity.WhoAmI;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class BatchIndexingJobIT {

	private static final Logger LOGGER = Logger.getLogger( BatchIndexingJobIT.class );

	// example dataset
	private final long DB_COMP_ROWS = 3;
	private final long DB_PERS_ROWS = 3;
	private final long DB_WHOS_ROWS = 3;
	private final long DB_TOTAL_ROWS = DB_COMP_ROWS + DB_PERS_ROWS + DB_WHOS_ROWS;

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
		List<WhoAmI> whos = Arrays.asList(
				new WhoAmI( "cid01", "id01", "uid01" ),
				new WhoAmI( "cid02", "id02", "uid02" ),
				new WhoAmI( "cid03", "id03", "uid03" ) );
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
			for ( WhoAmI w : whos ) {
				em.persist( w );
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
	public void testMassIndexer_usingAddRootEntities() throws InterruptedException,
			IOException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.purgeAll( WhoAmI.class );
		ftem.flushToIndexes();
		em.close();

		// searches before mass index,
		// expected no results for each search
		List<Company> companies = findClass( Company.class, "name", "Google" );
		List<Person> people = findClass( Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = findClass( WhoAmI.class, "id", "id01" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );
		assertEquals( 0, whos.size() );

		JobOperator jobOperator = JobFactory.getJobOperator();
		long executionId = BatchIndexingJob.forEntities( Company.class, Person.class, WhoAmI.class )
				.underJavaSE( emf, jobOperator )
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
		whos = findClass( WhoAmI.class, "id", "id01" );
		assertEquals( 1, companies.size() );
		assertEquals( 1, people.size() );
		assertEquals( 1, whos.size() );
	}

	@Test
	public void testMassIndexer_usingCriteria() throws InterruptedException,
			IOException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.flushToIndexes();
		em.close();

		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, findClass( Company.class, "name", "Google" ).size() );
		assertEquals( 0, findClass( Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, findClass( Company.class, "name", "Microsoft" ).size() );

		JobOperator jobOperator = JobFactory.getJobOperator();
		long executionId = BatchIndexingJob.forEntity( Company.class )
				.restrictedBy( Restrictions.in( "name", "Google", "Red Hat" ) )
				.underJavaSE( emf, jobOperator )
				.start();
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = keepTestAlive( jobExecution );

		assertEquals( 1, findClass( Company.class, "name", "Google" ).size() );
		assertEquals( 1, findClass( Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, findClass( Company.class, "name", "Microsoft" ).size() );
	}

	@Test
	public void testMassIndexer_usingHQL() throws InterruptedException,
			IOException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.flushToIndexes();
		em.close();

		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, findClass( Company.class, "name", "Google" ).size() );
		assertEquals( 0, findClass( Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, findClass( Company.class, "name", "Microsoft" ).size() );

		JobOperator jobOperator = JobFactory.getJobOperator();
		long executionId = BatchIndexingJob.forEntity( Company.class )
				.restrictedBy( "select c from Company c where c.name in ( 'Google', 'Red Hat' )" )
				.underJavaSE( emf, jobOperator )
				.start();
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = keepTestAlive( jobExecution );

		assertEquals( 1, findClass( Company.class, "name", "Google" ).size() );
		assertEquals( 1, findClass( Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, findClass( Company.class, "name", "Microsoft" ).size() );
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
						assertEquals( DB_TOTAL_ROWS, m.getValue() );
					}
					else if ( m.getType().equals( MetricType.WRITE_COUNT ) ) {
						assertEquals( DB_TOTAL_ROWS, m.getValue() );
					}
				}
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			default:
				break;
		}
	}

	/**
	 * Convert the Metric array contained in StepExecution to a key-value map for easy access to Metric parameters.
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
