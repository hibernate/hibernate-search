/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.persistence.EntityManagerFactory;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Mincong Huang
 */
@RunWith(MockitoJUnitRunner.class)
public class BatchIndexingJobTest {

	private static final boolean OPTIMIZE_AFTER_PURGE = true;
	private static final boolean OPTIMIZE_AT_END = true;
	private static final boolean PURGE_AT_START = true;
	private static final int FETCH_SIZE = 100000;
	private static final int MAX_RESULTS = 1000000;
	private static final int MAX_THREADS = 2;
	private static final int ROWS_PER_PARTITION = 500;

	@Mock
	private JobOperator mockedOperator;

	@Mock
	private EntityManagerFactory mockedEMF;

	@Before
	public void setUp() {
		Mockito.when( mockedOperator.start( Mockito.anyString(), Mockito.any( Properties.class ) ) )
				.thenReturn( 1L );
		Mockito.when( mockedEMF.isOpen() ).thenReturn( true );
	}

	@Test
	public void testJobParamsAll() throws IOException {

		ArgumentCaptor<Properties> propsCaptor = ArgumentCaptor.forClass( Properties.class );
		long executionID = BatchIndexingJob.forEntities( String.class, Integer.class )
				.underJavaSE( mockedEMF, mockedOperator )
				.fetchSize( FETCH_SIZE )
				.maxResults( MAX_RESULTS )
				.maxThreads( MAX_THREADS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeAtEnd( OPTIMIZE_AT_END )
				.rowsPerPartition( ROWS_PER_PARTITION )
				.purgeAtStart( PURGE_AT_START )
				.start();
		assertEquals( 1L, executionID );

		Mockito.verify( mockedOperator )
				.start( Mockito.anyString(), propsCaptor.capture() );
		Properties props = propsCaptor.getValue();
		assertEquals( FETCH_SIZE, Integer.parseInt( props.getProperty( "fetchSize" ) ) );
		assertEquals( MAX_RESULTS, Integer.parseInt( props.getProperty( "maxResults" ) ) );
		assertEquals( OPTIMIZE_AFTER_PURGE, Boolean.parseBoolean( props.getProperty( "optimizeAfterPurge" ) ) );
		assertEquals( OPTIMIZE_AT_END, Boolean.parseBoolean( props.getProperty( "optimizeAtEnd" ) ) );
		assertEquals( ROWS_PER_PARTITION, Integer.parseInt( props.getProperty( "rowsPerPartition" ) ) );
		assertEquals( PURGE_AT_START, Boolean.parseBoolean( props.getProperty( "purgeAtStart" ) ) );
		assertEquals( MAX_THREADS, Integer.parseInt( props.getProperty( "maxThreads" ) ) );

		String rootEntities = propsCaptor.getValue().getProperty( "rootEntities" );
		List<String> entityNames = Arrays.asList( rootEntities.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
		assertTrue( entityNames.contains( String.class.getName() ) );
	}

	@Test
	public void testForEntities_notNull() throws IOException {

		ArgumentCaptor<Properties> propsCaptor = ArgumentCaptor.forClass( Properties.class );
		long executionID = BatchIndexingJob.forEntities( Integer.class, String.class )
				.underJavaSE( mockedEMF, mockedOperator )
				.start();
		assertEquals( 1L, executionID );

		Mockito.verify( mockedOperator )
				.start( Mockito.anyString(), propsCaptor.capture() );
		String rootEntities = propsCaptor.getValue().getProperty( "rootEntities" );
		List<String> entityNames = Arrays.asList( rootEntities.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
		assertTrue( entityNames.contains( String.class.getName() ) );
	}

	@Test
	public void testForEntity_notNull() throws IOException {

		ArgumentCaptor<Properties> propsCaptor = ArgumentCaptor.forClass( Properties.class );
		long executionID = BatchIndexingJob.forEntity( Integer.class )
				.underJavaSE( mockedEMF, mockedOperator )
				.start();
		assertEquals( 1L, executionID );

		Mockito.verify( mockedOperator )
				.start( Mockito.anyString(), propsCaptor.capture() );
		String rootEntities = propsCaptor.getValue().getProperty( "rootEntities" );
		List<String> entityNames = Arrays.asList( rootEntities.split( "," ) );
		entityNames.forEach( entityName -> entityName = entityName.trim() );
		assertTrue( entityNames.contains( Integer.class.getName() ) );
	}

	@Test(expected = NullPointerException.class)
	public void testForEntity_null() {
		BatchIndexingJob.forEntity( null );
	}

	@Test(expected = NullPointerException.class)
	public void testForEntitiy_null() {
		BatchIndexingJob.forEntities( null );
	}

	@Test(expected = NullPointerException.class)
	public void testRestrictedBy_stringNull() {
		BatchIndexingJob.forEntity( String.class ).restrictedBy( (String) null );
	}

	@Test(expected = NullPointerException.class)
	public void testRestrictedBy_criterionNull() {
		BatchIndexingJob.forEntity( String.class ).restrictedBy( (Criterion) null );
	}

	/**
	 * A batch indexing job cannot have 2 types of restrictions in the same time. Either JPQL / HQL or Criteria approach
	 * is used. Using both will leads to illegal argument exception.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testRestrictedBy_twoRestrictionTypes() {
		BatchIndexingJob.forEntity( String.class )
				.restrictedBy( "from string" )
				.restrictedBy( Restrictions.isEmpty( "dummy" ) );
	}
}
