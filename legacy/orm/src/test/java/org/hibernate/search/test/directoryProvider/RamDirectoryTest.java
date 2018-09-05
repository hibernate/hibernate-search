/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.search.Search;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.AlternateDocument;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
@Category(SkipOnElasticsearch.class) // Directories are specific to the Lucene backend
public class RamDirectoryTest extends SearchInitializationTestBase {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void localHeap() throws Exception {
		doTest( "local-heap" );
	}

	@Test
	public void ram() throws Exception {
		logged.expectMessage( "HSEARCH000346" );
		doTest( "ram" );
	}

	private void doTest(String directoryProviderName) throws Exception {
		init( directoryProviderName, Document.class, AlternateDocument.class );
		Session s = getTestResourceManager().openSession();
		s.getTransaction().begin();
		Document document =
				new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" );
		s.persist( document );
		s.flush();
		s.persist(
				new AlternateDocument(
						document.getId(),
						"Hibernate in Action",
						"Object/relational mapping with Hibernate",
						"blah blah blah"
				)
		);
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, getDocumentNbr() );

		s = getTestResourceManager().openSession();
		s.getTransaction().begin();
		TermQuery q = new TermQuery( new Term( "alt_title", "hibernate" ) );
		List hibernateDocuments = Search.getFullTextSession( s ).createFullTextQuery( q, Document.class ).list();
		assertEquals(
				"does not properly filter", 0,
				hibernateDocuments.size()
		);
		assertEquals(
				"does not properly filter", 1,
				Search.getFullTextSession( s )
						.createFullTextQuery( q, Document.class, AlternateDocument.class )
						.list().size()
		);
		s.delete( s.get( AlternateDocument.class, document.getId() ) );
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, getDocumentNbr() );

		s = getTestResourceManager().openSession();
		s.getTransaction().begin();
		s.delete( s.createCriteria( Document.class ).uniqueResult() );
		s.getTransaction().commit();
		s.close();
	}

	private void init(String directoryProviderName, Class<?> ... classes) {
		Map<String, Object> settings = new HashMap<>();
		settings.put( "hibernate.search.default.directory_provider", directoryProviderName );
		init( new ImmutableTestConfiguration( settings, classes ) );
	}

	private int getDocumentNbr() throws Exception {
		return getBackendTestHelper().getNumberOfDocumentsInIndex( new PojoIndexedTypeIdentifier( Document.class ) );
	}

}
