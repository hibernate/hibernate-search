/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import java.util.Properties;

import org.junit.Assert;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchFactoryBuilder;
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
	private SearchFactoryImplementor sf;

	public SearchFactoryHolder(Class<?>... entities) {
		this( null, entities );
	}

	public SearchFactoryHolder(SearchMapping buildMappingDefinition, Class<?>... entities) {
		this.buildMappingDefinition = buildMappingDefinition;
		this.entities = entities;
		this.configuration = new Properties();
		this.configuration.setProperty( "hibernate.search.default.directory_provider", "ram" );
		this.configuration.setProperty( "hibernate.search.lucene_version", "LUCENE_CURRENT" );
	}

	public SearchFactoryImplementor getSearchFactory() {
		return sf;
	}

	@Override
	protected void before() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setProgrammaticMapping( buildMappingDefinition );
		for ( String key : configuration.stringPropertyNames() ) {
			cfg.addProperty( key, configuration.getProperty( key ) );
		}
		for ( Class<?> c : entities ) {
			cfg.addClass( c );
		}
		sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
	}

	@Override
	protected void after() {
		sf.close();
	}

	public SearchFactoryHolder withProperty(String key, Object value) {
		Assert.assertNull( "SessionFactory already initialized", sf );
		configuration.put( key, value );
		return this;
	}

	public AbstractWorkspaceImpl extractWorkspace(Class indexedType) {
		EntityIndexBinding indexBindingForEntity = sf.getIndexBinding( indexedType );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) indexManager.getBackendQueueProcessor();
		return backend.getIndexResources().getWorkspace();
	}

}
