/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.runtime.context.JobContext;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.steps.lucene.PartitionMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for partition plan validation.
 *
 * @author Mincong Huang
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ContextHelper.class)
public class PartitionMapperTest {

	private int COMP_ROWS = 500;
	private int PERS_ROWS = 5 * 1000;
	private Set<Class<?>> ROOT_ENTITIES = Stream.of( Company.class, Person.class )
			.collect( Collectors.toSet() );

	// mock jsr 352
	@Mock
	private JobContext jobContext;

	// mock environment set up
	@Mock
	private EntityManagerFactory emf;
	@Mock
	private SessionFactory sessionFactory;
	@Mock
	private Session session;
	@Mock
	private StatelessSession ss;

	// mock scrollable and criteria
	@Mock
	private ScrollableResults idScrollC;
	@Mock
	private ScrollableResults idScrollP;
	@Mock
	private Criteria criteriaC;
	@Mock
	private Criteria criteriaP;

	// mock methods inside ContextHelper
	@Mock
	private ExtendedSearchIntegrator extendedSearchIntegrator;
	@Mock
	private Map<Class<?>, EntityIndexBinding> indexBindings;
	@Mock
	private EntityIndexBinding indexBindingC;
	@Mock
	private EntityIndexBinding indexBindingP;
	@Mock
	private DocumentBuilderIndexedEntity docBuilderC;
	@Mock
	private DocumentBuilderIndexedEntity docBuilderP;

	@InjectMocks
	private PartitionMapper partitionMapper;

	@Before
	public void setUp() {

		// mock job context
		JobContextData jobData = new JobContextData( ROOT_ENTITIES );
		Mockito.when( jobContext.getTransientUserData() ).thenReturn( jobData );

		// mock factories
		Mockito.when( emf.unwrap( Mockito.any() ) ).thenReturn( sessionFactory );
		Mockito.when( sessionFactory.openSession() ).thenReturn( session );
		Mockito.when( sessionFactory.openStatelessSession() ).thenReturn( ss );

		// mock criteria for class Company
		Mockito.when( ss.createCriteria( Company.class ) ).thenReturn( criteriaC );
		Mockito.when( session.createCriteria( Company.class ) ).thenReturn( criteriaC );
		Mockito.when( criteriaC.addOrder( Mockito.any( Order.class ) ) ).thenReturn( criteriaC );
		Mockito.when( criteriaC.setCacheable( Mockito.anyBoolean() ) ).thenReturn( criteriaC );
		Mockito.when( criteriaC.setFetchSize( Mockito.anyInt() ) ).thenReturn( criteriaC );
		Mockito.when( criteriaC.setProjection( Mockito.any( Projection.class ) ) )
				.thenReturn( criteriaC );
		Mockito.when( criteriaC.setReadOnly( Mockito.anyBoolean() ) ).thenReturn( criteriaC );
		Mockito.when( criteriaC.scroll( ScrollMode.FORWARD_ONLY ) ).thenReturn( idScrollC );
		Mockito.when( criteriaC.uniqueResult() ).thenReturn( (Object) ( COMP_ROWS * 1L ) );
		Mockito.when( idScrollC.get( 0 ) )
				.thenReturn( 500 ); // companyID = 500, divider 1 (last)
		Mockito.when( idScrollC.scroll( Mockito.anyInt() ) )
				.thenReturn( true )
				.thenReturn( false );

		// mock criteria for class Person
		Mockito.when( ss.createCriteria( Person.class ) ).thenReturn( criteriaP );
		Mockito.when( session.createCriteria( Person.class ) ).thenReturn( criteriaP );
		Mockito.when( criteriaP.addOrder( Mockito.any( Order.class ) ) ).thenReturn( criteriaP );
		Mockito.when( criteriaP.setCacheable( Mockito.anyBoolean() ) ).thenReturn( criteriaP );
		Mockito.when( criteriaP.setFetchSize( Mockito.anyInt() ) ).thenReturn( criteriaP );
		Mockito.when( criteriaP.setProjection( Mockito.any( Projection.class ) ) )
				.thenReturn( criteriaP );
		Mockito.when( criteriaP.setReadOnly( Mockito.anyBoolean() ) ).thenReturn( criteriaP );
		Mockito.when( criteriaP.scroll( ScrollMode.FORWARD_ONLY ) ).thenReturn( idScrollP );
		Mockito.when( criteriaP.uniqueResult() ).thenReturn( (Object) ( PERS_ROWS * 1L ) );
		Mockito.when( idScrollP.get( 0 ) )
				.thenReturn( "P1000" ) // personID = P1000, divider 1
				.thenReturn( "P2000" ) // personID = P2000, divider 2
				.thenReturn( "P3000" ) // personID = P3000, divider 3
				.thenReturn( "P4000" ) // personID = P4000, divider 4
				.thenReturn( "P5000" ); // personID = P5000, divider 5 (last)
		Mockito.when( idScrollP.scroll( Mockito.anyInt() ) )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( false );

		// mock context helper
		PowerMockito.mockStatic( ContextHelper.class );
		Mockito.when( ContextHelper.getSearchintegrator( Mockito.any( Session.class ) ) )
				.thenReturn( extendedSearchIntegrator );
		Mockito.when( extendedSearchIntegrator.getIndexBindings() ).thenReturn( indexBindings );
		Mockito.when( indexBindings.get( Person.class ) ).thenReturn( indexBindingP );
		Mockito.when( indexBindingP.getDocumentBuilder() ).thenReturn( docBuilderP );
		Mockito.when( docBuilderP.getIdentifierName() ).thenReturn( "id" );
		Mockito.when( indexBindings.get( Company.class ) ).thenReturn( indexBindingC );
		Mockito.when( indexBindingC.getDocumentBuilder() ).thenReturn( docBuilderC );
		Mockito.when( docBuilderC.getIdentifierName() ).thenReturn( "id" );
	}

	/**
	 * Prove that there're N + 1 partitions for each root entity, where N stands
	 * for the ceiling number of the division between the rows to index and the
	 * max rows per partition, and the 1 stands for the tail partition for
	 * entities inserted after partitioning.
	 *
	 * @throws Exception
	 */
	@Test
	public void testMapPartitions() throws Exception {

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();
		int realPartitionC = 0; // companies
		int realPartitionP = 0; // people

		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( "entityName" );
			if ( entityName.equals( Company.class.toString() ) ) {
				realPartitionC++;
			}
			else if ( entityName.equals( Person.class.toString() ) ) {
				realPartitionP++;
			}
		}

		// nbPartitions = ceil( rows / rowsPerPartition ) + tailPartition
		// c = ceil( 500 / 1000 ) + 1 = 1 + 1 = 2
		// p = ceil( 5000 / 1000 ) + 1 = 5 + 1 = 6
		assertEquals( 2, realPartitionC );
		assertEquals( 6, realPartitionP );
	}
}
