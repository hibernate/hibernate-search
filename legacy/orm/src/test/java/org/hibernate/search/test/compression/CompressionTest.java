/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.compression;

import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // Compression is specific to the Lucene backend
public class CompressionTest extends SearchTestBase {

	/**
	 * Verifies the fields are really stored in compressed format
	 *
	 * @throws Exception in case the test fails
	 */
	@Test
	public void testFieldWasCompressed() throws Exception {
		IndexReader indexReader = getSearchFactory().getIndexReaderAccessor().open( LargeDocument.class );
		try {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			TopDocs topDocs = searcher.search( new MatchAllDocsQuery(), 10 );
			Assert.assertEquals( 1, topDocs.totalHits );

			ScoreDoc doc = topDocs.scoreDocs[0];
			Document document = indexReader.document( doc.doc );
			{
				IndexableField[] fields = document.getFields( "title" );
				assertEquals( 1, fields.length );
				assertNotNull( fields[0].fieldType().indexOptions() );
				assertTrue( fields[0].fieldType().stored() );
				assertFalse( isCompressed( fields[0] ) );
				assertEquals(
						"Hibernate in Action, third edition",
						fields[0].stringValue()
				);
			}
			{
				IndexableField[] fields = document.getFields( "abstract" );
				assertEquals( 1, fields.length );
				assertTrue( isCompressed( fields[0] ) );
				assertEquals(
						"<b>JPA2 with Hibernate</b>",
						restoreValue( fields[0] )
				);
			}
			{
				IndexableField[] fields = document.getFields( "text" );
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
	@Test
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
			QueryParser queryParser = new QueryParser( "", new SimpleAnalyzer() );
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
	@Test
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

	private String restoreValue(IndexableField field) throws DataFormatException {
		if ( field.binaryValue() != null ) {
			Assert.assertNull( "we rely on this in the Projection implementation", field.stringValue() );
			return CompressionTools.decompressString( field.binaryValue() );
		}
		else {
			return field.stringValue();
		}
	}

	private boolean isCompressed(IndexableField field) {
		if ( field.binaryValue() == null ) {
			return false;
		}
		else {
			try {
				CompressionTools.decompressString( field.binaryValue() );
				return true;
			}
			catch (DataFormatException e) {
				return false;
			}
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				LargeDocument.class
		};
	}

	@Override
	@Before
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
