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
package org.hibernate.search.test.programmaticmapping;

import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.junit.rules.ExternalResource;

/**
 * TestingSearchFactoryHolder.
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public class TestingSearchFactoryHolder extends ExternalResource {

	private final SearchMapping buildMappingDefinition;
	private SearchFactoryImplementor sf;
	private final Class<?>[] entities;

	public TestingSearchFactoryHolder(Class<?>... entities) {
		this( null, entities );
	}

	public TestingSearchFactoryHolder(SearchMapping buildMappingDefinition, Class<?>... entities) {
		this.buildMappingDefinition = buildMappingDefinition;
		this.entities = entities;
	}

	public SearchFactoryImplementor getSearchFactory() {
		return sf;
	}

	@Override
	protected void before() throws Throwable {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.setProgrammaticMapping( buildMappingDefinition );
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		for ( Class<?> c : entities ) {
			cfg.addClass( c );
		}
		sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
	}

	@Override
	protected void after() {
		sf.close();
	}

}
