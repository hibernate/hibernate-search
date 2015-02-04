/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IdUniquenessResolver;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.impl.HibernateStatelessInitializer;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Search configuration implementation wrapping an Hibernate Core configuration
 *
 * @author Emmanuel Bernard
 */
public class SearchConfigurationFromHibernateCore extends SearchConfigurationBase implements SearchConfiguration {

	private final org.hibernate.cfg.Configuration cfg;
	private final ClassLoaderService classLoaderService;
	private final Map<Class<? extends Service>, Object> providedServices;
	private ReflectionManager reflectionManager;


	public SearchConfigurationFromHibernateCore(org.hibernate.cfg.Configuration cfg,
			org.hibernate.boot.registry.classloading.spi.ClassLoaderService hibernateClassLoaderService) {
		// hmm, not sure why we throw NullPointerExceptions from these sanity checks
		// Shouldn't we use AssertionFailure or a log message + SearchException? (HF)
		if ( cfg == null ) {
			throw new NullPointerException( "Configuration is null" );
		}
		this.cfg = cfg;

		if ( hibernateClassLoaderService == null ) {
			throw new NullPointerException( "ClassLoaderService is null" );
		}
		this.classLoaderService = new DelegatingClassLoaderService( hibernateClassLoaderService );
		Map<Class<? extends Service>, Object> providedServices = new HashMap<>( 1 );
		providedServices.put( IdUniquenessResolver.class, new HibernateCoreIdUniquenessResolver( cfg ) );
		this.providedServices = Collections.unmodifiableMap( providedServices );
	}

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return new ClassIterator( cfg.getClassMappings() );
	}

	@Override
	public Class<?> getClassMapping(String name) {
		return cfg.getClassMapping( name ).getMappedClass();
	}

	@Override
	public String getProperty(String propertyName) {
		return cfg.getProperty( propertyName );
	}

	@Override
	public Properties getProperties() {
		return cfg.getProperties();
	}

	@Override
	public ReflectionManager getReflectionManager() {
		if ( reflectionManager == null ) {
			try {
				//TODO introduce a ReflectionManagerHolder interface to avoid reflection
				//I want to avoid hard link between HAN and Search for such a simple need
				//reuse the existing reflectionManager one when possible
				reflectionManager =
						(ReflectionManager) cfg.getClass().getMethod( "getReflectionManager" ).invoke( cfg );

			}
			catch (Exception e) {
				reflectionManager = new JavaReflectionManager();
			}
		}
		return reflectionManager;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return null;
	}

	@Override
	public Map<Class<? extends Service>, Object> getProvidedServices() {
		return providedServices;
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return HibernateStatelessInitializer.INSTANCE;
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return true;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	private static class ClassIterator implements Iterator<Class<?>> {
		private Iterator hibernatePersistentClassIterator;
		private Class<?> future;

		private ClassIterator(Iterator hibernatePersistentClassIterator) {
			this.hibernatePersistentClassIterator = hibernatePersistentClassIterator;
		}

		@Override
		public boolean hasNext() {
			//we need to read the next non null one. getMappedClass() can return null and should be ignored
			if ( future != null ) {
				return true;
			}
			do {
				if ( !hibernatePersistentClassIterator.hasNext() ) {
					future = null;
					return false;
				}
				final PersistentClass pc = (PersistentClass) hibernatePersistentClassIterator.next();
				future = pc.getMappedClass();
			}
			while ( future == null );
			return true;
		}

		@Override
		public Class<?> next() {
			//run hasNext to init the next element
			if ( !hasNext() ) {
				throw new NoSuchElementException();
			}
			Class<?> result = future;
			future = null;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException( "Cannot modify Hibernate Core metadata" );
		}
	}
}
