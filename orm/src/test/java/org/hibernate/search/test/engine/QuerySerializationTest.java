/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.hibernate.Session;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.engine.impl.DocumentExtractorImpl;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.SerializationTestHelper;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test is meant to verify that HSQuery implementation is able to
 * be serialized, deserialized and then still perform the query.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Ignore("See HSEARCH-1478")
public class QuerySerializationTest extends SearchTestBase {

	@Test
	public void testQueryObjectIsSerializable() throws IOException, ClassNotFoundException {
		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		Document document =
				new Document( "Hibernate OGM in Action", "Cloud mapping with Hibernate", "blah blah cloud blah cloud" );
		s.persist( document );
		s.getTransaction().commit();
		s.close();

		TermQuery query = new TermQuery( new Term( "Abstract", "hibernate" ) );

		FullTextSession fullTextSession = Search.getFullTextSession( s );
		SearchIntegrator searchFactory = (SearchIntegrator) fullTextSession.getSearchFactory();

		//this is *not* the standard way to create a Query:
		HSQuery hsQuery = searchFactory.createHSQuery()
				.luceneQuery( query )
				.targetedEntities( new ArrayList<Class<?>>() );
		int size = extractResultSize( hsQuery );

		assertEquals( "Should have found a match", 1, size );

		HSQuery hsQueryDuplicate = (HSQuery) SerializationTestHelper.duplicateBySerialization( hsQuery );
		hsQueryDuplicate.afterDeserialise( searchFactory );
		int sizeOfDuplicate = extractResultSize( hsQueryDuplicate );

		assertEquals( "Should have found a match", 1, sizeOfDuplicate );
	}

	private int extractResultSize(HSQuery hsQuery) {
		DocumentExtractorImpl documentExtractor = (DocumentExtractorImpl) hsQuery.queryDocumentExtractor();
		TopDocs topDocs = documentExtractor.getTopDocs();
		documentExtractor.close();
		return topDocs.totalHits;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Document.class,
				AlternateDocument.class
		};
	}

}
