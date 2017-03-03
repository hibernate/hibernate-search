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
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class MassIndexingJobParametersBuilderTest {

	private static final String SESSION_FACTORY_NAME = "someUniqueString";

	private static final boolean OPTIMIZE_AFTER_PURGE = true;
	private static final boolean OPTIMIZE_AT_END = true;
	private static final boolean PURGE_AT_START = true;
	private static final int FETCH_SIZE = 100000;
	private static final int MAX_RESULTS = 1000000;
	private static final int MAX_THREADS = 2;
	private static final int ROWS_PER_PARTITION = 500;

	@Test
	public void testJobParamsAll() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntities( String.class, Integer.class )
				.entityManagerFactoryReference( SESSION_FACTORY_NAME )
				.fetchSize( FETCH_SIZE )
				.maxResults( MAX_RESULTS )
				.maxThreads( MAX_THREADS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeAtEnd( OPTIMIZE_AT_END )
				.rowsPerPartition( ROWS_PER_PARTITION )
				.purgeAtStart( PURGE_AT_START )
				.build();

		assertEquals( SESSION_FACTORY_NAME, props.getProperty( MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE ) );
		assertEquals( FETCH_SIZE, Integer.parseInt( props.getProperty( MassIndexingJobParameters.FETCH_SIZE ) ) );
		assertEquals( MAX_RESULTS, Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_RESULTS ) ) );
		assertEquals( OPTIMIZE_AFTER_PURGE, Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE ) ) );
		assertEquals( OPTIMIZE_AT_END, Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.OPTIMIZE_AT_END ) ) );
		assertEquals( ROWS_PER_PARTITION, Integer.parseInt( props.getProperty( MassIndexingJobParameters.ROWS_PER_PARTITION ) ) );
		assertEquals( PURGE_AT_START, Boolean.parseBoolean( props.getProperty( MassIndexingJobParameters.PURGE_AT_START ) ) );
		assertEquals( MAX_THREADS, Integer.parseInt( props.getProperty( MassIndexingJobParameters.MAX_THREADS ) ) );

		String rootEntities = props.getProperty( MassIndexingJobParameters.ROOT_ENTITIES );
		List<String> entityNames = Arrays.asList( rootEntities.split( "," ) );
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

		String rootEntities = props.getProperty( MassIndexingJobParameters.ROOT_ENTITIES );
		List<String> entityNames = Arrays.asList( rootEntities.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
		assertTrue( entityNames.contains( String.class.getName() ) );
	}

	@Test
	public void testForEntity_notNull() throws IOException {
		Properties props = MassIndexingJob.parameters()
				.forEntity( Integer.class )
				.build();

		String rootEntities = props.getProperty( MassIndexingJobParameters.ROOT_ENTITIES );
		List<String> entityNames = Arrays.asList( rootEntities.split( "," ) );
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
}
