/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.test.engine;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.hibernate.Session;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.SerializationTestHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test is meant to verify that HSQuery implementation is able to
 * be serialized, deserialized and then still perform the query.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class QuerySerializationTest extends SearchTestCaseJUnit4 {

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
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();

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
		DocumentExtractor documentExtractor = hsQuery.queryDocumentExtractor();
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
