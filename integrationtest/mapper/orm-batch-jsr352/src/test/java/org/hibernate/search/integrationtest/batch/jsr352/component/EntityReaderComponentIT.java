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

import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.CacheMode;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.IndexScope;
import org.hibernate.search.batch.jsr352.core.massindexing.step.spi.EntityReader;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.util.BackendConfigurations;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Single-component test for item reader validation.
 *
 * @author Mincong Huang
 */
public class EntityReaderComponentIT {

	private static final List<Company> COMPANIES = Arrays.asList(
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" )
	);

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private EntityManagerFactory emf;

	private JobContext mockedJobContext;

	private StepContext mockedStepContext;

	private EntityReader entityReader;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( Company.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false );
	}

	@Before
	public void init() {
		emf = setupHolder.entityManagerFactory();

		setupHolder.runInTransaction( session -> COMPANIES.forEach( session::persist ) );

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
