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

package org.hibernate.search.test.query.nullValues;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.query.ProjectionToMapResultTransformer;

/**
 * Tests for indexing and querying {@code null} values. See HSEARCh-115
 *
 * @author Hardy Ferentschik
 */
public class ProgrammaticIndexAndQueryNullTest extends SearchTestCase {

	public void testProjectedValueGetsConvertedToNull() throws Exception {
		ProgrammaticConfiguredValue nullValue = new ProgrammaticConfiguredValue( null );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		getSession().save( nullValue );
		tx.commit();

		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		parser.setAllowLeadingWildcard( true );
		Query query = parser.parse( "*" );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, ProgrammaticConfiguredValue.class );
		fullTextQuery.setProjection(
				"id",
				"value",
				FullTextQuery.DOCUMENT
		);
		fullTextQuery.setResultTransformer( new ProjectionToMapResultTransformer() );
		List mappedResults = fullTextQuery.list();
		assertTrue( "Wrong result size", mappedResults.size() == 1 );

		Map map = (Map) mappedResults.get( 0 );
		Integer id = (Integer) map.get( "id" );
		assertNotNull( id );

		String value = (String) map.get( "value" );
		assertEquals( "The null token should be converted back to null", null, value );

		Document doc = (Document) map.get( FullTextQuery.DOCUMENT );
		assertEquals(
				"The programmatically configured null value should be in the document",
				"@null@",
				doc.getFieldable( "value" ).stringValue()
		);

		tx.commit();
		fullTextSession.close();
	}

	private SearchMapping createSearchMapping() {
		SearchMapping mapping = new SearchMapping();

		mapping.entity( ProgrammaticConfiguredValue.class )
				.indexed()
				.property( "id", ElementType.FIELD ).documentId().name( "id" )
				.property( "value", ElementType.FIELD ).field().store( Store.YES ).indexNullAs( "@null@" );
		return mapping;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( Environment.MODEL_MAPPING, createSearchMapping() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ProgrammaticConfiguredValue.class
		};
	}
}
