/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for item reader validation.
 *
 * @author Mincong Huang
 */
public class EntityReaderTest {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final String PERSISTENCE_UNIT_NAME = "h2";
	private static final Company[] COMPANIES = new Company[]{
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" ) };
	private EntityManagerFactory emf;

	@Mock
	private JobContext mockedJobContext;

	@Mock
	private StepContext mockedStepContext;

	@InjectMocks
	private EntityReader entityReader;

	@Before
	public void setUp() {
		EntityManager em = null;
		try {
			emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			for ( Company c : COMPANIES ) {
				em.persist( c );
			}
			em.getTransaction().commit();
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				log.error( e );
			}
		}

		final String cacheable = String.valueOf( false );
		final String entityName = Company.class.getName();
		final String fetchSize = String.valueOf( 1000 );
		final String hql = null;
		final String maxResults = String.valueOf( Integer.MAX_VALUE );
		final String partitionId = String.valueOf( 0 );
		entityReader = new EntityReader( cacheable,
				entityName,
				fetchSize,
				hql,
				maxResults,
				partitionId,
				null,
				null
				);

		MockitoAnnotations.initMocks( this );
	}

	@Test
	public void testReadItem_withoutBoundary() throws Exception {
		// mock job context
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		jobData.setCustomQueryCriteria( new HashSet<>() );
		jobData.setEntityTypes( Company.class );
		Mockito.when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		// mock step context
		Mockito.doNothing().when( mockedStepContext ).setTransientUserData( Mockito.any() );

		entityReader.open( null );
		for ( int i = 0; i < COMPANIES.length; i++ ) {
			Company c = (Company) entityReader.readItem();
			assertEquals( COMPANIES[i].getName(), c.getName() );
		}
		// no more item
		assertNull( entityReader.readItem() );
	}
}
