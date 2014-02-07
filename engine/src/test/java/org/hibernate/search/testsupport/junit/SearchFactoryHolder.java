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
