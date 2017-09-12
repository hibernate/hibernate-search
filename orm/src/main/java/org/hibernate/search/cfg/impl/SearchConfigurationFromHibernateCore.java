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
import java.util.Set;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IdUniquenessResolver;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.orm.loading.impl.HibernateStatelessInitializer;
import org.hibernate.search.engine.service.beanresolver.spi.BeanResolver;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.named.spi.NamedResolver;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.hcore.impl.HibernateSessionFactoryService;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Search configuration implementation wrapping an Hibernate Core configuration
 *
 * @author Emmanuel Bernard
 */
public class SearchConfigurationFromHibernateCore extends SearchConfigurationBase implements SearchConfiguration {

	private final ConfigurationService configurationService;
	private final ClassLoaderService classLoaderService;
	private final BeanResolver beanResolver;
	private final Map<Class<? extends Service>, Object> providedServices;
	private final Metadata metadata;
	private final Properties legacyConfigurationProperties;//For compatibility reasons only. Should be removed? See HSEARCH-1890
	private final boolean multitenancyEnabled;

	private ReflectionManager reflectionManager;

	public SearchConfigurationFromHibernateCore(Metadata metadata, ConfigurationService configurationService,
			org.hibernate.boot.registry.classloading.spi.ClassLoaderService hibernateOrmClassLoaderService,
			org.hibernate.search.hcore.spi.BeanResolver hibernateOrmBeanResolver,
			HibernateSessionFactoryService sessionService, JndiService namingService) {
		this.metadata = metadata;
		// hmm, not sure why we throw NullPointerExceptions from these sanity checks
		// Shouldn't we use AssertionFailure or a log message + SearchException? (HF)
		if ( configurationService == null ) {
			throw new NullPointerException( "Configuration is null" );
		}
		this.configurationService = configurationService;

		if ( hibernateOrmClassLoaderService == null ) {
			throw new NullPointerException( "ClassLoaderService is null" );
		}
		this.classLoaderService = new DelegatingClassLoaderService( hibernateOrmClassLoaderService );
		this.beanResolver = hibernateOrmBeanResolver != null ? new DelegatingBeanResolver( hibernateOrmBeanResolver ) : null;

		Map<Class<? extends Service>, Object> providedServices = new HashMap<>( 1 );
		providedServices.put( IdUniquenessResolver.class, new HibernateCoreIdUniquenessResolver( metadata ) );
		providedServices.put( HibernateSessionFactoryService.class, sessionService );
		providedServices.put( NamedResolver.class, new DelegatingNamedResolver( namingService ) );
		this.providedServices = Collections.unmodifiableMap( providedServices );
		this.legacyConfigurationProperties = extractProperties( configurationService );

		MultiTenancyStrategy multitenancyStrategy =
				sessionService.getSessionFactory().getSessionFactoryOptions().getMultiTenancyStrategy();
		this.multitenancyEnabled = !MultiTenancyStrategy.NONE.equals( multitenancyStrategy );
	}

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return new ClassIterator( metadata.getEntityBindings().iterator() );
	}

	@Override
	public Class<?> getClassMapping(String entityName) {
		return metadata.getEntityBinding( entityName ).getMappedClass();
	}

	@Override
	public String getProperty(String propertyName) {
		return configurationService.getSetting( propertyName, org.hibernate.engine.config.spi.StandardConverters.STRING );
	}

	@Override
	public Properties getProperties() {
		return this.legacyConfigurationProperties;
	}

	@Override
	public ReflectionManager getReflectionManager() {
		if ( reflectionManager == null ) {
			if ( metadata instanceof MetadataImplementor ) {
				reflectionManager = ((MetadataImplementor) metadata).getMetadataBuildingOptions().getReflectionManager();
			}
			if ( reflectionManager == null ) {
				// Fall back to our own instance of JavaReflectionManager
				// when metadata is not a MetadataImplementor or
				// the reflection manager were not created by Hibernate yet.
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
	public boolean isMultitenancyEnabled() {
		return multitenancyEnabled;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public BeanResolver getBeanResolver() {
		return beanResolver;
	}

	private static class ClassIterator implements Iterator<Class<?>> {
		private Iterator<PersistentClass> hibernatePersistentClassIterator;
		private Class<?> future;

		private ClassIterator(Iterator<PersistentClass> hibernatePersistentClassIterator) {
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
				final PersistentClass pc = hibernatePersistentClassIterator.next();
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

	private static Properties extractProperties(final ConfigurationService configurationService) {
		Properties props = new Properties();
		Set<Map.Entry> entrySet = configurationService.getSettings().entrySet();
		for ( Map.Entry entry : entrySet ) {
			final Object key = entry.getKey();
			if ( key instanceof String ) {
				props.put( key, entry.getValue() );
			}
		}
		return props;
	}

	@Override
	public boolean isJPAAnnotationsProcessingEnabled() {
		return true;
	}

}
