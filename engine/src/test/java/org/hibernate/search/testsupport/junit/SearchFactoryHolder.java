/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Assert;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.rules.ExternalResource;

/**
 * TestingSearchFactoryHolder.
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public class SearchFactoryHolder extends ExternalResource {

	private final SearchMapping buildMappingDefinition;
	private final Class<?>[] entities;
	private final Properties configuration;
	private final Map<Class<? extends Service>,Service> providedServices = new HashMap<>();

	private SearchIntegrator[] searchIntegrator;
	private int numberOfSessionFactories = 1;

	public SearchFactoryHolder(Class<?>... entities) {
		this( null, entities );
	}

	public SearchFactoryHolder(SearchMapping buildMappingDefinition, Class<?>... entities) {
		this.buildMappingDefinition = buildMappingDefinition;
		this.entities = entities;
		this.configuration = new Properties();
	}

	public ExtendedSearchIntegrator getSearchFactory() {
		return searchIntegrator[0].unwrap( ExtendedSearchIntegrator.class );
	}

	@Override
	protected void before() throws Throwable {
		searchIntegrator = new SearchIntegrator[numberOfSessionFactories];
		for ( int i = 0; i < numberOfSessionFactories; i++ ) {
			searchIntegrator[i] = createSearchFactory();
		}
	}

	private SearchIntegrator createSearchFactory() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setProgrammaticMapping( buildMappingDefinition );
		for ( Entry<Class<? extends Service>, Service> entry : providedServices.entrySet() ) {
			cfg.addProvidedService( entry.getKey(), entry.getValue() );
		}
		for ( String key : configuration.stringPropertyNames() ) {
			cfg.addProperty( key, configuration.getProperty( key ) );
		}
		for ( Class<?> c : entities ) {
			cfg.addClass( c );
		}
		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	@Override
	protected void after() {
		if ( searchIntegrator != null ) {
			for ( SearchIntegrator sf : searchIntegrator ) {
				sf.close();
			}
		}
	}

	public SearchFactoryHolder withProperty(String key, Object value) {
		Assert.assertNull( "SearchIntegrator already initialized", searchIntegrator );
		configuration.put( key, value );
		return this;
	}

	public <T extends Service> SearchFactoryHolder withService(Class<T> serviceType, T serviceInstance) {
		providedServices.put( serviceType, serviceInstance );
		return this;
	}

	public AbstractWorkspaceImpl extractWorkspace(Class indexedType) {
		EntityIndexBinding indexBindingForEntity = getSearchFactory().getIndexBinding( indexedType );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) indexManager.getBackendQueueProcessor();
		return backend.getIndexResources().getWorkspace();
	}

	/**
	 * Allows to construct multiple copies of the SearchFactory.
	 * Each of them will have identical configuration and share the instances of the provided services.
	 * Most other helpers provided by this class will access only the first SearchFactory, unless
	 * they accept a specific index argument.
	 */
	public SearchFactoryHolder multipleInstances(int clusterNodes) {
		if ( clusterNodes < 1 ) {
			throw new SearchException( "Can not construct less than one node" );
		}
		this.numberOfSessionFactories = clusterNodes;
		return this;
	}

}
