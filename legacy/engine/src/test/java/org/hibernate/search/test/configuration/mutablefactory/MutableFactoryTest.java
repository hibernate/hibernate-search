/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.mutablefactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.configuration.mutablefactory.generated.Generated;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
public class MutableFactoryTest {

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private SearchIntegrator searchIntegrator;

	private final SearchITHelper helper = new SearchITHelper( () -> this.searchIntegrator );

	@Test
	public void testCreateEmptyFactory() throws Exception {
		integratorResource.create( new SearchConfigurationForTest() );
	}

	@Test
	public void testAddingClassFullModel() throws Exception {
		searchIntegrator = integratorResource.create( new SearchConfigurationForTest() );
		searchIntegrator = integratorResource.create( searchIntegrator, A.class );

		helper.index( new A( 1, "Emmanuel" ), 1 );

		helper.assertThat( "name", "emmanuel" )
				.from( A.class )
				.hasResultSize( 1 );

		searchIntegrator = integratorResource.create( searchIntegrator, B.class );

		helper.index( new B( 1, "Noel" ), 1 );

		helper.assertThat( "name", "noel" )
				.from( B.class )
				.hasResultSize( 1 );
	}

	@Test
	public void testAddingClassSimpleAPI() throws Exception {
		searchIntegrator = integratorResource.create( new SearchConfigurationForTest() );

		searchIntegrator.addClasses( A.class );

		helper.index( new A( 1, "Emmanuel" ), 1 );

		helper.assertThat( "name", "emmanuel" )
				.from( A.class )
				.hasResultSize( 1 );

		searchIntegrator.addClasses( B.class, C.class );

		helper.index()
				.push( new B( 1, "Noel" ), 1 )
				.push( new C( 1, "Vincent" ), 1 )
				.execute();

		helper.assertThat( "name", "noel" )
				.from( B.class )
				.hasResultSize( 1 );

		helper.assertThat( "name", "vincent" )
				.from( C.class )
				.hasResultSize( 1 );
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2421 Support statistics with Elasticsearch
	public void testAddingClassSimpleAPIwithJMX() throws Exception {
		searchIntegrator = integratorResource.create(
						new SearchConfigurationForTest()
							.addClass( A.class )
							.addProperty( "hibernate.search.jmx_enabled" , Boolean.TRUE.toString() )
							.addProperty( "hibernate.search.generate_statistics", Boolean.TRUE.toString() )
							.addProperty( "com.sun.management.jmxremote", Boolean.TRUE.toString() ) );

		helper.index( new A( 1, "Emmanuel" ), 1 );

		helper.assertThat( "name", "emmanuel" )
				.from( A.class )
				.hasResultSize( 1 );
		assertEquals( 1 , searchIntegrator.getStatistics().getSearchQueryExecutionCount() );
		assertEquals( 1 , searchIntegrator.getStatistics().getIndexedClassNames().size() );

		searchIntegrator.addClasses( B.class, C.class );

		helper.index()
				.push( new B( 1, "Noel" ), 1 )
				.push( new C( 1, "Vincent" ), 1 )
				.execute();

		helper.assertThat( "name", "noel" )
				.from( A.class, B.class, C.class )
				.hasResultSize( 1 );
		assertEquals( 2 , searchIntegrator.getStatistics().getSearchQueryExecutionCount() );
		assertEquals( 3 , searchIntegrator.getStatistics().getIndexedClassNames().size() );

		helper.assertThat( "name", "vincent" )
				.from( A.class, B.class, C.class )
				.hasResultSize( 1 );
		assertEquals( 3 , searchIntegrator.getStatistics().getSearchQueryExecutionCount() );
		assertEquals( 3 , searchIntegrator.getStatistics().getIndexedClassNames().size() );
	}

	@Test
	public void testMultiThreadedAddClasses() throws Exception {
		searchIntegrator = integratorResource.create( new SearchConfigurationForTest() );
		int numberOfClasses = 100;
		int numberOfThreads = 10;
		new ConcurrentRunner( numberOfClasses, numberOfThreads,
				new TaskFactory() {
					@Override
					public Runnable createRunnable(int i) throws Exception {
						return new DoAddClass( i );
					}
				}
			)
			.setTimeout( 1, TimeUnit.MINUTES )
			.execute();

		for ( int i = 0; i < numberOfClasses; i++ ) {
			final Class<?> classByNumber = getClassByNumber( i, searchIntegrator.getServiceManager() );
			helper.assertThat( "name", "emmanuel" + i )
					.from( classByNumber )
					.hasResultSize( 1 );
		}
	}

	private static Class<?> getClassByNumber(int i, ServiceManager serviceManager) throws ClassNotFoundException {
		ClassLoaderService classLoaderService = serviceManager.getClassLoaderService();
		Class<?> clazz = classLoaderService.classForName(
					Generated.A0.class.getName().replace( "A0", "A" + i )
			);
		return clazz;
	}

	private class DoAddClass implements Runnable {
		private final int index;

		public DoAddClass(int index) {
			this.index = index;
		}

		@Override
		public void run() {
			try {
				String name = "Emmanuel" + index;
				final Class<?> aClass = MutableFactoryTest.getClassByNumber(
						index,
						searchIntegrator.getServiceManager()
				);

				System.err.println( "Creating index #" + index + " for class " + aClass );
				searchIntegrator.addClasses( aClass );
				Object entity = aClass.getConstructor( Integer.class, String.class )
						.newInstance( index, name );
				helper.index( entity, index );

				EntityIndexBinding indexBindingForEntity = searchIntegrator.getIndexBindings().get( aClass );
				assertNotNull( indexBindingForEntity );

				Set<IndexManager> indexManagers = indexBindingForEntity.getIndexManagerSelector().all();
				assertEquals( 1, indexManagers.size() );

				helper.assertThat( "name", "emmanuel" + index )
						.from( aClass )
						.hasResultSize( 1 );
			}
			catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException
					| IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException( "Unexpected exception while manipulating dynamically created classes", e );
			}
		}
	}

}
