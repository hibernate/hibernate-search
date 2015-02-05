/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.manualsource.WorkLoad;
import org.hibernate.search.manualsource.WorkLoadManager;
import org.hibernate.search.manualsource.source.IdExtractor;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;

/**
 * Mimmics SessionFactoryObserver from the ORM module
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
//TODO enable JMX beans?
public class WorkLoadManagerImpl implements WorkLoadManager {
	static {
		Version.touch();
	}

	private ExtendedSearchIntegrator searchIntegrator;
	private IdExtractor idExtractor;

	public WorkLoadManagerImpl(List<Class<?>> classes, IdExtractor idExtractor, Properties properties) {
		SearchIntegrator builtSearchIntegrator = new SearchIntegratorBuilder()
				.configuration( new WorkLoadSearchConfiguration( classes, properties ) )
				.buildSearchIntegrator();
		this.searchIntegrator = builtSearchIntegrator.unwrap( ExtendedSearchIntegrator.class );
		this.idExtractor = idExtractor;
	}

	@Override
	public WorkLoad createWorkLoad() {
		return new WorkLoadImpl( this );
	}

	@Override
	public void close() {
		if ( searchIntegrator != null ) {
			searchIntegrator.close();
		}
	}

	// getter method used by WorkLoadImpl
	public ExtendedSearchIntegrator getSearchIntegrator() {
		return searchIntegrator;
	}

	// getter method used by WorkLoadImpl
	public IdExtractor getIdExtractor() {
		return idExtractor;
	}

	private static class WorkLoadSearchConfiguration extends SearchConfigurationBase {
		private final List<Class<?>> classes;
		private final Properties properties;
		private ClassLoaderService classLoaderService = new DefaultClassLoaderService();

		public WorkLoadSearchConfiguration(List<Class<?>> classes, Properties properties) {
			this.classes = classes;
			this.properties = properties;
		}

		@Override
		public Iterator<Class<?>> getClassMappings() {
			return classes.iterator();
		}

		@Override
		public Class<?> getClassMapping(String name) {
			//TODO a bit nasty I guess to use the classloader
			Class<?> clazz = classLoaderService.classForName( name );
			if ( classes.contains( clazz ) ) {
				return clazz;
			}
			else {
				return null;
			}
		}

		@Override
		public String getProperty(String propertyName) {
			return properties.getProperty( propertyName );
		}

		@Override
		public Properties getProperties() {
			return properties;
		}

		@Override
		public ReflectionManager getReflectionManager() {
			return null;
		}

		@Override
		public SearchMapping getProgrammaticMapping() {
			return null;
		}

		@Override
		public Map<Class<? extends Service>, Object> getProvidedServices() {
			return Collections.emptyMap();
		}

		@Override
		public ClassLoaderService getClassLoaderService() {
			return classLoaderService;
		}
	}
}
