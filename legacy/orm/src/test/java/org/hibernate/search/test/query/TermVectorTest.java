/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * @author John Griffin
 * @author Sanne Grinovero
 */
@Category(SkipOnElasticsearch.class) // IndexReaders (which provide access to term vectors) are specific to the Lucene backend
public class TermVectorTest extends SearchTestBase {

	@Test
	public void testPositionOffsets() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		createIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();

		// Here's how to get a reader from a FullTextSession
		SearchFactory searchFactory = s.getSearchFactory();
		IndexReader reader = searchFactory.getIndexReaderAccessor().open( ElectricalProperties.class );

		/**
		 * Since there are so many combinations of results here, we are only going
		 * to assert a few. - J.G.
		 */
		int x = 0; // only Document zero is tested: asserts rely on natural document order
		Terms termVector = reader.getTermVector( x, "content" );
		assertNotNull( termVector );
		TermsEnum iterator = termVector.iterator();
		BytesRef next = iterator.next(); //move to first Document: we expect it to exist
		assertNotNull( next );
		long totalTermFreq = iterator.totalTermFreq();

		assertEquals( "electrical", next.utf8ToString() );
		assertEquals( 2, totalTermFreq );

		final DocsAndPositionsEnum docsAndPositions = iterator.docsAndPositions( null, null );
		docsAndPositions.advance( 0 );//move to Document id 0

		docsAndPositions.nextPosition();
		assertEquals( 0, docsAndPositions.startOffset() );//first term in sentence
		assertEquals( 10, docsAndPositions.endOffset() );

		docsAndPositions.nextPosition();
		assertEquals( 29, docsAndPositions.startOffset() );//Term is mentioned again at character 29

		// cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		searchFactory.getIndexReaderAccessor().close( reader );
		tx.commit();
		s.close();
	}

	@Test
	public void testNoTermVector() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Employee e1 = new Employee( 1000, "Griffin", "ITech" );
		s.save( e1 );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		// Here's how to get a reader from a FullTextSession
		SearchFactory searchFactory = s.getSearchFactory();
		IndexReader reader = searchFactory.getIndexReaderAccessor().open( Employee.class );

		Terms termVector = reader.getTermVector( 0, "dept" );
		assertNull( "should not find a term position vector", termVector );

		// cleanup
		for ( Object element : s.createQuery( "from " + ElectricalProperties.class.getName() ).list() ) {
			s.delete( element );
		}
		searchFactory.getIndexReaderAccessor().close( reader );
		tx.commit();
		s.close();
	}

	private void createIndex(FullTextSession s) {
		storeSeparately( s, new ElectricalProperties( 1000, "Electrical Engineers measure Electrical Properties" ) );
		storeSeparately( s, new ElectricalProperties( 1001, "Electrical Properties are interesting" ) );
		storeSeparately( s, new ElectricalProperties( 1002, "Electrical Properties are measurable properties" ) );
	}

	// test relies on the order in which Lucene Documents are written, so we commit
	// the transaction after each entity is saved to make sure the backend doesn't reorder work.
	private void storeSeparately(FullTextSession s, ElectricalProperties ep) {
		Transaction tx = s.beginTransaction();
		s.save( ep );
		tx.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ElectricalProperties.class, Employee.class };
	}
}
