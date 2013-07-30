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
package org.hibernate.search.test.compression;

import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.junit.Assert;

/**
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class CompressionTest extends SearchTestCase {

	/**
	 * Verifies the fields are really stored in compressed format
	 *
	 * @throws Exception in case the test fails
	 */
	public void testFieldWasCompressed() throws Exception {
		IndexReader indexReader = getSearchFactory().getIndexReaderAccessor().open( LargeDocument.class );
		try {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			TopDocs topDocs = searcher.search( new MatchAllDocsQuery(), null, 10 );
			Assert.assertEquals( 1, topDocs.totalHits );

			ScoreDoc doc = topDocs.scoreDocs[0];
			Document document = indexReader.document( doc.doc );
			{
				Fieldable[] fields = document.getFieldables( "title" );
				assertEquals( 1, fields.length );
				assertTrue( fields[0].isIndexed() );
				assertTrue( fields[0].isStored() );
				assertFalse( isCompressed( fields[0] ) );
				assertEquals(
						"Hibernate in Action, third edition",
						fields[0].stringValue()
				);
			}
			{
				Fieldable[] fields = document.getFieldables( "abstract" );
				assertEquals( 1, fields.length );
				assertTrue( isCompressed( fields[0] ) );
				assertEquals(
						"<b>JPA2 with Hibernate</b>",
						restoreValue( fields[0] )
				);
			}
			{
				Fieldable[] fields = document.getFieldables( "text" );
				assertEquals( 1, fields.length );
				assertTrue( isCompressed( fields[0] ) );
				assertEquals(
						"This is a placeholder for the new text that you should write",
						restoreValue( fields[0] )
				);
			}
		}
		finally {
			getSearchFactory().getIndexReaderAccessor().close( indexReader );
		}
	}

	/**
	 * Verifies the compressed fields are also searchable.
	 *
	 * @throws Exception in case the test fails
	 */
	public void testCompressedFieldSearch() throws Exception {
		assertFindsN( 1, "title:third" );
		assertFindsN( 1, "abstract:jpa2" );
		assertFindsN( 1, "text:write" );
		assertFindsN( 0, "text:jpa2" );
	}

	private void assertFindsN(int expectedToFind, String queryString) throws ParseException {
		openSession().beginTransaction();
		try {
			FullTextSession fullTextSession = Search.getFullTextSession( getSession() );
			QueryParser queryParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "", new SimpleAnalyzer( TestConstants.getTargetLuceneVersion() ) );
			Query query = queryParser.parse( queryString );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
					query,
					LargeDocument.class
			);
			@SuppressWarnings("unchecked")
			List<LargeDocument> list = fullTextQuery.list();
			Assert.assertEquals( expectedToFind, list.size() );
			if ( expectedToFind == 1 ) {
				Assert.assertEquals( "Hibernate in Action, third edition", list.get( 0 ).getTitle() );
			}
		}
		finally {
			getSession().getTransaction().commit();
			getSession().close();
		}
	}

	/**
	 * Verify that projection is able to inflate stored data
	 */
	public void testProjectionOnCompressedFields() {
		openSession().beginTransaction();
		try {
			FullTextSession fullTextSession = Search.getFullTextSession( getSession() );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
					new MatchAllDocsQuery(),
					LargeDocument.class
			);
			List list = fullTextQuery.setProjection( "title", "abstract", "text" ).list();
			Assert.assertEquals( 1, list.size() );
			Object[] results = (Object[]) list.get( 0 );
			Assert.assertEquals( "Hibernate in Action, third edition", results[0] );
			Assert.assertEquals( "JPA2 with Hibernate", results[1] );
			Assert.assertEquals( "This is a placeholder for the new text that you should write", results[2] );
		}
		finally {
			getSession().getTransaction().commit();
			getSession().close();
		}
	}

	private String restoreValue(Fieldable field) throws DataFormatException {
		if ( field.isBinary() ) {
			Assert.assertNull( "we rely on this in the Projection implementation", field.stringValue() );
			return CompressionTools.decompressString( field.getBinaryValue() );
		}
		else {
			return field.stringValue();
		}
	}

	private boolean isCompressed(Fieldable field) {
		if ( !field.isBinary() ) {
			return false;
		}
		else {
			try {
				CompressionTools.decompressString( field.getBinaryValue() );
				return true;
			}
			catch (DataFormatException e) {
				return false;
			}
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				LargeDocument.class
		};
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session s = openSession();
		s.getTransaction().begin();
		s.persist(
				new LargeDocument(
						"Hibernate in Action, third edition",
						"JPA2 with Hibernate",
						"This is a placeholder for the new text that you should write"
				)
		);
		s.getTransaction().commit();
		s.close();
	}
}
