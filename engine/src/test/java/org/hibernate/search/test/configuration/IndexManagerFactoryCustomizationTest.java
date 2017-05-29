/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.impl.DefaultIndexManagerFactory;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.CollectionHelper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test to verify pluggability of an alternative {@code IndexManagerFactory}
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1211")
@Category(SkipOnElasticsearch.class) // Configuration in the Elasticsearch modules messes with the defaults
public class IndexManagerFactoryCustomizationTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testDefaultImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		verifyIndexManagerTypeIs( DirectoryBasedIndexManager.class, cfg );
	}

	@Test
	public void testOverriddenDefaultImplementation() {
		SearchConfigurationForTest configurationForTest = new SearchConfigurationForTest();

		Map<Class<? extends Service>, String> fakedDiscoveredServices = CollectionHelper.newHashMap( 1 );
		fakedDiscoveredServices.put( IndexManagerFactory.class, NRTIndexManagerFactory.class.getName() );
		configurationForTest.setClassLoaderService( new CustomClassLoaderService( fakedDiscoveredServices ) );

		verifyIndexManagerTypeIs( NRTIndexManager.class, configurationForTest );
	}

	private void verifyIndexManagerTypeIs(Class<? extends IndexManager> expectedIndexManagerClass, SearchConfigurationForTest cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Document.class ).indexed().indexName( "documents" )
				.property( "id", ElementType.FIELD ).documentId()
				.property( "title", ElementType.FIELD ).field();

		cfg.setProgrammaticMapping( mapping );
		cfg.addClass( Document.class );
		SearchIntegrator sf = integratorResource.create( cfg );
		Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "documents" ) );
		// trigger a SearchFactory rebuild:
		sf.addClasses( Dvd.class );
		// and verify the option is not lost:
		Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "dvds" ) );
		Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "documents" ) );
	}

	private Class<? extends IndexManager> extractDocumentIndexManagerClassName(SearchIntegrator si, String indexName) {
		ExtendedSearchIntegrator factoryImplementor = si.unwrap( ExtendedSearchIntegrator.class );
		IndexManager indexManager = factoryImplementor.getIndexManagerHolder().getIndexManager( indexName );
		Assert.assertNotNull( indexManager );
		return indexManager.getClass();
	}

	public static final class Document {
		long id;
		String title;
	}

	@Indexed(index = "dvds")
	public static final class Dvd {
		@DocumentId
		long id;
		@Field
		String title;
	}

	public static class CustomClassLoaderService implements ClassLoaderService {

		private final ClassLoaderService defaultClassLoaderService;
		private final Map<Class<? extends Service>, String> fakedDiscoveredServices;

		public CustomClassLoaderService(Map<Class<? extends Service>, String> fakedDiscoveredServices) {
			this.defaultClassLoaderService = new DefaultClassLoaderService();
			this.fakedDiscoveredServices = fakedDiscoveredServices;
		}

		@Override
		public <T> Class<T> classForName(String className) {
			return defaultClassLoaderService.classForName( className );
		}

		@Override
		public URL locateResource(String name) {
			return defaultClassLoaderService.locateResource( name );
		}

		@Override
		public InputStream locateResourceStream(String name) {
			return defaultClassLoaderService.locateResourceStream( name );
		}

		@Override
		public <T> Iterable<T> loadJavaServices(Class<T> serviceContract) {
			if ( fakedDiscoveredServices.containsKey( serviceContract ) ) {
				LinkedHashSet<T> services = new LinkedHashSet<T>( 1 );
				Class<T> clazz = classForName( fakedDiscoveredServices.get( serviceContract ) );
				services.add( ClassLoaderHelper.instanceFromClass( serviceContract, clazz, "fake service" ) );
				return services;
			}
			return defaultClassLoaderService.loadJavaServices( serviceContract );
		}
	}

	public static class NRTIndexManagerFactory extends DefaultIndexManagerFactory {
		@Override
		public IndexManager createDefaultIndexManager() {
			return new NRTIndexManager();
		}
	}

}
