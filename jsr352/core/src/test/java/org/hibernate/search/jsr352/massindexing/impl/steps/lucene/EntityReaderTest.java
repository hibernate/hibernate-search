/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.CacheMode;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import org.junit.After;
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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String PERSISTENCE_UNIT_NAME = "primary_pu";
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

		final String cacheMode = CacheMode.IGNORE.name();
		final String entityName = Company.class.getName();
		final String entityFetchSize = String.valueOf( 1000 );
		final String checkpointInterval = String.valueOf( 1000 );
		final String sessionClearInterval = String.valueOf( 100 );
		final String hql = null;
		final String maxResults = String.valueOf( Integer.MAX_VALUE );
		final String partitionId = String.valueOf( 0 );
		entityReader = new EntityReader( cacheMode,
				entityName,
				entityFetchSize,
				checkpointInterval,
				sessionClearInterval,
				hql,
				maxResults,
				partitionId,
				null,
				null,
				IndexScope.FULL_ENTITY.name() );

		MockitoAnnotations.initMocks( this );
	}

	@After
	public void shutDown() {
		if ( emf.isOpen() ) {
			emf.close();
		}
	}

	@Test
	public void testReadItem_withoutBoundary() throws Exception {
		// mock job context
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		jobData.setCustomQueryCriteria( new HashSet<>() );
		jobData.setEntityTypeDescriptors( Arrays.asList( JobTestUtil.createSimpleEntityTypeDescriptor( emf, Company.class ) ) );
		Mockito.when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		// mock step context
		Mockito.doNothing().when( mockedStepContext ).setTransientUserData( Mockito.any() );

		try {
			entityReader.open( null );
			for ( int i = 0; i < COMPANIES.length; i++ ) {
				Company c = (Company) entityReader.readItem();
				assertEquals( COMPANIES[i].getName(), c.getName() );
			}
			// no more item
			assertNull( entityReader.readItem() );
		}
		finally {
			entityReader.close();
		}
	}
}
