/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.hibernate.CacheMode;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class MassIndexingJobParametersBuilderTest {

	private static final String SESSION_FACTORY_NAME = "someUniqueString";

	private static final String TENANT_ID = "myTenantId";

	private static final boolean MERGE_SEGMENTS_AFTER_PURGE = true;
	private static final boolean MERGE_SEGMENTS_ON_FINISH = true;
	private static final boolean PURGE_ALL_ON_START = true;
	private static final int ID_FETCH_SIZE = Integer.MIN_VALUE;
	private static final int ENTITY_FETCH_SIZE = Integer.MIN_VALUE + 1;
	private static final int MAX_RESULTS_PER_ENTITY = 10_000;
	private static final int MAX_THREADS = 2;
	private static final int ROWS_PER_PARTITION = 500;
	private static final int CHECKPOINT_INTERVAL = 200;
	private static final CacheMode CACHE_MODE = CacheMode.GET;

	@Test
	public void testJobParamsAll() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntities( String.class, Integer.class )
				.entityManagerFactoryReference( SESSION_FACTORY_NAME )
				.idFetchSize( ID_FETCH_SIZE )
				.entityFetchSize( ENTITY_FETCH_SIZE )
				.maxResultsPerEntity( MAX_RESULTS_PER_ENTITY )
				.maxThreads( MAX_THREADS )
				.mergeSegmentsAfterPurge( MERGE_SEGMENTS_AFTER_PURGE )
				.mergeSegmentsOnFinish( MERGE_SEGMENTS_ON_FINISH )
				.rowsPerPartition( ROWS_PER_PARTITION )
				.checkpointInterval( CHECKPOINT_INTERVAL )
				.purgeAllOnStart( PURGE_ALL_ON_START )
				.cacheMode( CACHE_MODE )
				.tenantId( TENANT_ID )
				.build();

		assertThat( props.getProperty( MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE ) ).isEqualTo( SESSION_FACTORY_NAME );
		assertThat( Integer.parseInt( props.getProperty( MassIndexingJobParameters.ID_FETCH_SIZE ) ) ).isEqualTo( ID_FETCH_SIZE );
		assertThat( Integer.parseInt( props.getProperty( MassIndexingJobParameters.ENTITY_FETCH_SIZE ) ) ).isEqualTo( ENTITY_FETCH_SIZE );
		assertThat( Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY ) ) ).isEqualTo( MAX_RESULTS_PER_ENTITY );
		assertThat( Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE ) ) ).isEqualTo( MERGE_SEGMENTS_AFTER_PURGE );
		assertThat( Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH ) ) ).isEqualTo( MERGE_SEGMENTS_ON_FINISH );
		assertThat( Integer.parseInt( props.getProperty( MassIndexingJobParameters.ROWS_PER_PARTITION ) ) ).isEqualTo( ROWS_PER_PARTITION );
		assertThat( Integer.parseInt( props.getProperty( MassIndexingJobParameters.CHECKPOINT_INTERVAL ) ) ).isEqualTo( CHECKPOINT_INTERVAL );
		assertThat( Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.PURGE_ALL_ON_START ) ) ).isEqualTo( PURGE_ALL_ON_START );
		assertThat( Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_THREADS ) ) ).isEqualTo( MAX_THREADS );
		assertThat( CacheMode.valueOf( props.getProperty( MassIndexingJobParameters.CACHE_MODE ) ) ).isEqualTo( CACHE_MODE );
		assertThat( props.getProperty( MassIndexingJobParameters.TENANT_ID ) ).isEqualTo( TENANT_ID );

		String entityTypes = props.getProperty( MassIndexingJobParameters.ENTITY_TYPES );
		List<String> entityNames = Arrays.asList( entityTypes.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertThat( entityNames.contains( Integer.class.getName() ) );
		assertThat( entityNames.contains( String.class.getName() ) );
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
		assertThat( entityNames.contains( Integer.class.getName() ) ).isTrue();
		assertThat( entityNames.contains( String.class.getName() ) ).isTrue();
	}

	@Test
	public void testForEntity_notNull() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntity( Integer.class )
				.build();

		String entityTypes = props.getProperty( MassIndexingJobParameters.ENTITY_TYPES );
		List<String> entityNames = Arrays.asList( entityTypes.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertThat( entityNames.contains( Integer.class.getName() ) );
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
