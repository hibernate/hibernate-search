/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.exception.SearchException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Mincong Huang
 */
public class MassIndexingJobParametersBuilderTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final String SESSION_FACTORY_NAME = "someUniqueString";

	private static final String TENANT_ID = "myTenantId";

	private static final boolean OPTIMIZE_AFTER_PURGE = true;
	private static final boolean OPTIMIZE_ON_FINISH = true;
	private static final boolean PURGE_ALL_ON_START = true;
	private static final int FETCH_SIZE = 100000;
	private static final int MAX_RESULTS_PER_ENTITY = 10_000;
	private static final int MAX_THREADS = 2;
	private static final int ROWS_PER_PARTITION = 500;

	@Test
	public void testJobParamsAll() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntities( String.class, Integer.class )
				.entityManagerFactoryReference( SESSION_FACTORY_NAME )
				.fetchSize( FETCH_SIZE )
				.maxResultsPerEntity( MAX_RESULTS_PER_ENTITY )
				.maxThreads( MAX_THREADS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeOnFinish( OPTIMIZE_ON_FINISH )
				.rowsPerPartition( ROWS_PER_PARTITION )
				.purgeAllOnStart( PURGE_ALL_ON_START )
				.tenantId( TENANT_ID )
				.build();

		assertEquals( SESSION_FACTORY_NAME, props.getProperty( MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE ) );
		assertEquals( FETCH_SIZE, Integer.parseInt( props.getProperty( MassIndexingJobParameters.FETCH_SIZE ) ) );
		assertEquals( MAX_RESULTS_PER_ENTITY, Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY ) ) );
		assertEquals( OPTIMIZE_AFTER_PURGE, Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE ) ) );
		assertEquals( OPTIMIZE_ON_FINISH, Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.OPTIMIZE_ON_FINISH ) ) );
		assertEquals( ROWS_PER_PARTITION, Integer.parseInt( props.getProperty( MassIndexingJobParameters.ROWS_PER_PARTITION ) ) );
		assertEquals( PURGE_ALL_ON_START, Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.PURGE_ALL_ON_START ) ) );
		assertEquals( MAX_THREADS, Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_THREADS ) ) );
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

	@Test(expected = NullPointerException.class)
	public void testRestrictedBy_criterionNull() {
		MassIndexingJob.parameters().forEntity( String.class ).restrictedBy( (Criterion) null );
	}

	@Test(expected = SearchException.class)
	public void testCheckpointInterval_greaterThanRowsPerPartitions() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.checkpointInterval( 5 )
				.rowsPerPartition( 4 )
				.build();
	}

	@Test(expected = SearchException.class)
	public void testCheckpointInterval_equalToRowsPerPartitions() {
		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.checkpointInterval( 4 )
				.rowsPerPartition( 4 )
				.build();
	}

	/**
	 * A batch indexing job cannot have 2 types of restrictions in the same time. Either JPQL / HQL or Criteria approach
	 * is used. Using both will leads to illegal argument exception.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testRestrictedBy_twoRestrictionTypes() {
		MassIndexingJob.parameters()
				.forEntity( String.class )
				.restrictedBy( "from string" )
				.restrictedBy( Restrictions.isEmpty( "dummy" ) );
	}

	@Test
	public void testTenantId_null() throws Exception {
		thrown.expect( NullPointerException.class );
		thrown.expectMessage( "Your tenantId is null, please provide a valid tenant ID." );

		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.tenantId( null );
	}

	@Test
	public void testTenantId_empty() throws Exception {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "Your tenantId is empty, please provide a valid tenant ID." );

		MassIndexingJob.parameters()
				.forEntity( UnusedEntity.class )
				.tenantId( "" );
	}

	private static class UnusedEntity {
		private UnusedEntity() {
		}
	}

}
