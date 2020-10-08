/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.component;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.CacheMode;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
import org.hibernate.search.util.common.SearchException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class MassIndexingJobParametersBuilderIT {

	private static final String SESSION_FACTORY_NAME = "someUniqueString";

	private static final String TENANT_ID = "myTenantId";

	private static final boolean OPTIMIZE_AFTER_PURGE = true;
	private static final boolean OPTIMIZE_ON_FINISH = true;
	private static final boolean PURGE_ALL_ON_START = true;
	private static final int ID_FETCH_SIZE = Integer.MIN_VALUE;
	private static final int ENTITY_FETCH_SIZE = Integer.MIN_VALUE + 1;
	private static final int MAX_RESULTS_PER_ENTITY = 10_000;
	private static final int MAX_THREADS = 2;
	private static final int ROWS_PER_PARTITION = 500;
	private static final int CHECKPOINT_INTERVAL = 200;
	private static final CacheMode CACHE_MODE = CacheMode.GET;

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();
	private EntityManagerFactory emf;

	@Before
	public void setUp() throws Exception {
		emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
	}

	@After
	public void tearDown() throws Exception {
		if ( emf != null ) {
			emf.close();
			emf = null;
		}
	}

	@Test
	public void testJobParamsAll() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntities( String.class, Integer.class )
				.entityManagerFactoryReference( SESSION_FACTORY_NAME )
				.idFetchSize( ID_FETCH_SIZE )
				.entityFetchSize( ENTITY_FETCH_SIZE )
				.maxResultsPerEntity( MAX_RESULTS_PER_ENTITY )
				.maxThreads( MAX_THREADS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeOnFinish( OPTIMIZE_ON_FINISH )
				.rowsPerPartition( ROWS_PER_PARTITION )
				.checkpointInterval( CHECKPOINT_INTERVAL )
				.purgeAllOnStart( PURGE_ALL_ON_START )
				.cacheMode( CACHE_MODE )
				.tenantId( TENANT_ID )
				.build();

		assertEquals(
				SESSION_FACTORY_NAME, props.getProperty( MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE ) );
		assertEquals( ID_FETCH_SIZE, Integer.parseInt( props.getProperty( MassIndexingJobParameters.ID_FETCH_SIZE ) ) );
		assertEquals(
				ENTITY_FETCH_SIZE,
				Integer.parseInt( props.getProperty( MassIndexingJobParameters.ENTITY_FETCH_SIZE ) )
		);
		assertEquals(
				MAX_RESULTS_PER_ENTITY,
				Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY ) )
		);
		assertEquals(
				OPTIMIZE_AFTER_PURGE,
				Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE ) )
		);
		assertEquals(
				OPTIMIZE_ON_FINISH,
				Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.OPTIMIZE_ON_FINISH ) )
		);
		assertEquals(
				ROWS_PER_PARTITION,
				Integer.parseInt( props.getProperty( MassIndexingJobParameters.ROWS_PER_PARTITION ) )
		);
		assertEquals(
				CHECKPOINT_INTERVAL,
				Integer.parseInt( props.getProperty( MassIndexingJobParameters.CHECKPOINT_INTERVAL ) )
		);
		assertEquals(
				PURGE_ALL_ON_START,
				Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.PURGE_ALL_ON_START ) )
		);
		assertEquals( MAX_THREADS, Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_THREADS ) ) );
		assertEquals( CACHE_MODE, CacheMode.valueOf( props.getProperty( MassIndexingJobParameters.CACHE_MODE ) ) );
		assertEquals( TENANT_ID, props.getProperty( MassIndexingJobParameters.TENANT_ID ) );

		String entityTypes = props.getProperty( MassIndexingJobParameters.ENTITY_TYPES );
		List<String> entityNames = Arrays.asList( entityTypes.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
		assertTrue( entityNames.contains( String.class.getName() ) );
	}

	@Test
	public void testForEntities_notNull() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntities( Integer.class, String.class )
				.entityManagerFactoryReference( SESSION_FACTORY_NAME )
				.build();

		String entityTypes = props.getProperty( MassIndexingJobParameters.ENTITY_TYPES );
		List<String> entityNames = Arrays.asList( entityTypes.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
		assertTrue( entityNames.contains( String.class.getName() ) );
	}

	@Test
	public void testForEntity_notNull() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntity( Integer.class )
				.build();

		String entityTypes = props.getProperty( MassIndexingJobParameters.ENTITY_TYPES );
		List<String> entityNames = Arrays.asList( entityTypes.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testForEntity_null() {
		MassIndexingJob.parameters().forEntity( null );
	}

	@Test(expected = NullPointerException.class)
	public void testRestrictedBy_stringNull() {
		MassIndexingJob.parameters().forEntity( String.class ).restrictedBy( (String) null );
	}

	@Test(expected = SearchException.class)
	public void testSessionClearInterval_greaterThanCheckpointInterval() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.sessionClearInterval( 5 )
				.checkpointInterval( 4 )
				.build();
	}

	@Test
	public void testSessionClearInterval_defaultGreaterThanCheckpointInterval() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.checkpointInterval( MassIndexingJobParameters.Defaults.SESSION_CLEAR_INTERVAL_DEFAULT_RAW - 1 )
				.build();
		// ok, session clear interval will default to the value of checkpointInterval
	}

	@Test(expected = SearchException.class)
	public void testSessionClearInterval_greaterThanDefaultCheckpointInterval() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.sessionClearInterval( MassIndexingJobParameters.Defaults.CHECKPOINT_INTERVAL_DEFAULT_RAW + 1 )
				.build();
	}

	@Test(expected = SearchException.class)
	public void testCheckpointInterval_greaterThanRowsPerPartitions() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.checkpointInterval( 5 )
				.rowsPerPartition( 4 )
				.build();
	}

	@Test
	public void testCheckpointInterval_defaultGreaterThanRowsPerPartitions() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.rowsPerPartition( MassIndexingJobParameters.Defaults.CHECKPOINT_INTERVAL_DEFAULT_RAW - 1 )
				.build();
		// ok, checkpoint interval will default to the value of rowsPerPartition
	}

	@Test(expected = SearchException.class)
	public void testCheckpointInterval_greaterThanDefaultRowsPerPartitions() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.checkpointInterval( MassIndexingJobParameters.Defaults.ROWS_PER_PARTITION + 1 )
				.build();
	}

	@Test
	public void testTenantId_null() throws Exception {
		assertThatThrownBy( () -> MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.tenantId( null ) )
				.isInstanceOf( NullPointerException.class )
				.hasMessage( "Your tenantId is null, please provide a valid tenant ID." );
	}

	@Test
	public void testTenantId_empty() throws Exception {
		assertThatThrownBy( () -> MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.tenantId( "" ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessage( "Your tenantId is empty, please provide a valid tenant ID." );
	}

	@Test
	public void testIdFetchSize() throws Exception {
		for ( int allowedValue : Arrays.asList( Integer.MAX_VALUE, 0, Integer.MIN_VALUE ) ) {
			MassIndexingJob.parameters()
					.forEntity( UnusedEntity.class )
					.idFetchSize( allowedValue );
		}
	}

	@Test
	public void testEntityFetchSize() throws Exception {
		for ( int allowedValue : Arrays.asList( Integer.MAX_VALUE, 0, Integer.MIN_VALUE ) ) {
			MassIndexingJob.parameters()
					.forEntity( UnusedEntity.class )
					.entityFetchSize( allowedValue );
		}
	}

	private static class UnusedEntity {
		private UnusedEntity() {
		}
	}

}
