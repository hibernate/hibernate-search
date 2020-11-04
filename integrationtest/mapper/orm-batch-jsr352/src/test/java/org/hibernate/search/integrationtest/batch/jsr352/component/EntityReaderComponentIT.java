/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.CacheMode;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.IndexScope;
import org.hibernate.search.batch.jsr352.core.massindexing.step.spi.EntityReader;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Single-component test for item reader validation.
 *
 * @author Mincong Huang
 */
public class EntityReaderComponentIT {

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	private static final List<Company> COMPANIES = Arrays.asList(
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" )
	);

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

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

		mockedJobContext = mock( JobContext.class );
		mockedStepContext = mock( StepContext.class );

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
		jobData.setEntityTypeDescriptors( Arrays.asList( JobTestUtil.createSimpleEntityTypeDescriptor( emf, Company.class ) ) );

		when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );
		mockedStepContext.setTransientUserData( any() );

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
