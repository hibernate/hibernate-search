/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.configuration;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Assert;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.impl.DefaultIndexManagerFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.CollectionHelper;
import org.junit.Test;

/**
 * Test to verify pluggability of an alternative {@code IndexManagerFactory}
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1211")
public class IndexManagerFactoryCustomizationTest {

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
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Document.class );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		try {
			Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "documents" ) );
			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class );
			// and verify the option is not lost:
			Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "dvds" ) );
			Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "documents" ) );
		}
		finally {
			sf.close();
		}
	}

	private Class<? extends IndexManager> extractDocumentIndexManagerClassName(SearchFactoryImplementor sf, String indexName) {
		IndexManager indexManager = sf.getIndexManagerHolder().getIndexManager( indexName );
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
		@SuppressWarnings("unchecked")
		public <T> LinkedHashSet<T> loadJavaServices(Class<T> serviceContract) {
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
