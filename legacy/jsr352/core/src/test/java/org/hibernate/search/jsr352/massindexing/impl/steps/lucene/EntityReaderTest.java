/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.CacheMode;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.test.util.JobTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for item reader validation.
 *
 * @author Mincong Huang
 */
public class EntityReaderTest {

	private static final String PERSISTENCE_UNIT_NAME = "primary_pu";

	private static final List<Company> COMPANIES = Arrays.asList(
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" )
	);

	private EntityManagerFactory emf;

	private JobContext mockedJobContext;

	private StepContext mockedStepContext;

	private EntityReader entityReader;

	@Before
	public void setUp() {
		EntityManager em = null;
		try {
			emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			COMPANIES.forEach( em::persist );
			em.getTransaction().commit();
		}
		finally {
			if ( em != null ) {
				em.close();
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

		mockedJobContext = niceMock( JobContext.class );
		mockedStepContext = niceMock( StepContext.class );

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
				IndexScope.FULL_ENTITY.name(),
				mockedJobContext,
				mockedStepContext );
	}

	@After
	public void shutDown() {
		if ( emf.isOpen() ) {
			emf.close();
		}
	}

	@Test
	public void testReadItem_withoutBoundary() throws Exception {
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		jobData.setCustomQueryCriteria( new HashSet<>() );
		jobData.setEntityTypeDescriptors( Arrays.asList( JobTestUtil.createSimpleEntityTypeDescriptor( emf, Company.class ) ) );

		expect( mockedJobContext.getTransientUserData() ).andReturn( jobData );
		mockedStepContext.setTransientUserData( anyObject() );
		replay( mockedJobContext, mockedStepContext );

		try {
			entityReader.open( null );
			for ( Company expected : COMPANIES ) {
				Company actual = (Company) entityReader.readItem();
				assertEquals( expected.getName(), actual.getName() );
			}
			// no more item
			assertNull( entityReader.readItem() );
		}
		finally {
			entityReader.close();
		}
	}
}
