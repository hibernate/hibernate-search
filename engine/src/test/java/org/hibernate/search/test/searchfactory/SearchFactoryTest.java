/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.searchfactory;

import java.util.Set;

import org.hibernate.search.SearchException;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.junit.Test;

import static java.lang.annotation.ElementType.FIELD;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class SearchFactoryTest {

	@Test
	public void testEntityWithNoDocumentIdThrowsException() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Entity.class );

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Entity.class ).indexed()
		;
		cfg.setProgrammaticMapping( mapping );

		try {
			new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
			fail( "Invalid configuration should have thrown an exception" );
		}
		catch ( SearchException e ) {
			assertTrue( e.getMessage().startsWith( "HSEARCH000177" ) );
		}
	}

	@Test
	public void testGetIndexedEntities() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Entity.class );

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Entity.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		Set<Class<?>> indexedClasses = sf.getIndexedEntities();
		assertTrue( indexedClasses.size() == 1 );
		assertTrue( indexedClasses.iterator().next().equals( Entity.class ) );
	}

	@Test
	public void tesNoIndexedEntities() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Entity.class );

		SearchMapping mapping = new SearchMapping();
		cfg.setProgrammaticMapping( mapping );

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		Set<Class<?>> indexedClasses = sf.getIndexedEntities();
		assertTrue( indexedClasses.isEmpty() );
	}

	public static class Entity {
		private long id;
	}
}


