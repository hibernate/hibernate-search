/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.analyzer;

import java.lang.annotation.ElementType;

import org.apache.solr.analysis.GermanStemFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Tests for HSEARCH-569.
 *
 * @author Hardy Ferentschik
 */
public class DuplicatedAnalyzerDefinitionTest extends SearchTestCase {

	public static final Log log = LoggerFactory.make();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { };
	}

	public void testDuplicatedAnalyzerDefinitionThrowsException() throws Exception {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Entity1.class );
		config.addAnnotatedClass( Entity2.class );
		config.setProperty( "hibernate.search.default.directory_provider", "ram" );
		try {
			config.buildSessionFactory();
			fail( "Session creation should have failed due to duplicate analyzer definition" );
		}
		catch (SearchException e) {
			assertTrue(
					"Multiple analyzer definitions with the same name: my-analyzer"
					.equals( e.getMessage() )
			);
		}
	}

	public void testDuplicatedProgrammaticAnalyzerDefinitionThrowsException() throws Exception {
		Configuration config = new Configuration();
		config.getProperties().put( Environment.MODEL_MAPPING, createSearchMapping() );
		config.setProperty( "hibernate.search.default.directory_provider", "ram" );
		try {
			config.buildSessionFactory();
			fail( "Session creation should have failed due to duplicate analyzer definition" );
		}
		catch (SearchException e) {
			assertTrue(
					"Multiple analyzer definitions with the same name: english"
					.equals( e.getMessage() )
			);
		}
	}

	private SearchMapping createSearchMapping() {
		SearchMapping mapping = new SearchMapping();

		mapping.analyzerDef( "english", StandardTokenizerFactory.class )
				.filter( LowerCaseFilterFactory.class )
				.filter( SnowballPorterFilterFactory.class )
				.analyzerDef(
						"english", StandardTokenizerFactory.class
				) // ups duplicate name here - this should throw an exception
				.filter( LowerCaseFilterFactory.class )
				.filter( GermanStemFilterFactory.class )
				.entity( BlogEntry.class )
				.indexed()
				.property( "title", ElementType.METHOD );
		return mapping;
	}
}


