/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.factory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.lucene.util.Version;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.genericjpa.factory.impl.SubClassSupportInstanceInitializer;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Manually defines the configuration. Classes and properties are the only implemented options at the moment
 *
 * @author Martin Braun (adaption), Emmanuel Bernard
 */
public class StandaloneSearchConfiguration extends SearchConfigurationBase implements SearchConfiguration {

	private final Logger LOGGER = Logger.getLogger( StandaloneSearchConfiguration.class.getName() );
	private final Map<String, Class<?>> classes;
	private final Properties properties;
	private final HashMap<Class<? extends Service>, Object> providedServices;
	private final InstanceInitializer initializer;
	private SearchMapping programmaticMapping;
	private boolean transactionsExpected = true;
	// TODO: is this correct?
	private boolean indexMetadataComplete = true;
	private boolean idProvidedImplicit = false;
	private ClassLoaderService classLoaderService;
	private ReflectionManager reflectionManager;

	public StandaloneSearchConfiguration() {
		this( new Properties() );
	}

	public StandaloneSearchConfiguration(Properties properties) {
		this( SubClassSupportInstanceInitializer.INSTANCE, properties );
	}

	public StandaloneSearchConfiguration(InstanceInitializer init) {
		this( new Properties() );
	}

	public StandaloneSearchConfiguration(InstanceInitializer init, Properties properties) {
		this.initializer = init;
		this.classes = new HashMap<>();
		this.properties = properties;
		// default values if nothing was explicitly set
		this.properties.computeIfAbsent(
				"hibernate.search.default.directory_provider", (key) -> {
					LOGGER.info( "defaulting to RAM directory-provider" );
					return "ram";
				}
		);
		this.properties.computeIfAbsent(
				"hibernate.search.lucene_version", (key) -> {
					LOGGER.info( "defaulting to Lucene Version: " + Version.LUCENE_5_3_0.toString() );
					return Version.LUCENE_5_3_0.toString();
				}
		);
		this.reflectionManager = new JavaReflectionManager();
		this.providedServices = new HashMap<>();
		this.classLoaderService = new DefaultClassLoaderService();
	}

	public StandaloneSearchConfiguration addProperty(String key, String value) {
		properties.setProperty( key, value );
		return this;
	}

	public StandaloneSearchConfiguration addClass(Class<?> indexed) {
		classes.put( indexed.getName(), indexed );
		return this;
	}

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return classes.values().iterator();
	}

	@Override
	public Class<?> getClassMapping(String name) {
		return classes.get( name );
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
		return this.reflectionManager;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return programmaticMapping;
	}

	public StandaloneSearchConfiguration setProgrammaticMapping(SearchMapping programmaticMapping) {
		this.programmaticMapping = programmaticMapping;
		return this;
	}

	@Override
	public Map<Class<? extends Service>, Object> getProvidedServices() {
		return providedServices;
	}

	public void addProvidedService(Class<? extends Service> serviceRole, Object service) {
		providedServices.put( serviceRole, service );
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return this.transactionsExpected;
	}

	public void setTransactionsExpected(boolean transactionsExpected) {
		this.transactionsExpected = transactionsExpected;
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return initializer;
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return indexMetadataComplete;
	}

	public void setIndexMetadataComplete(boolean indexMetadataComplete) {
		this.indexMetadataComplete = indexMetadataComplete;
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return idProvidedImplicit;
	}

	public StandaloneSearchConfiguration setIdProvidedImplicit(boolean idProvidedImplicit) {
		this.idProvidedImplicit = idProvidedImplicit;
		return this;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	public void setClassLoaderService(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

}
