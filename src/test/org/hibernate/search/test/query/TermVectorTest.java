package org.hibernate.search.test.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author John Griffin
 */
public class TermVectorTest extends SearchTestCase {
	private static Log log = LogFactory.getLog( TermVectorTest.class );

	public void testPositionOffsets() throws Exception {
		FullTextSession s = Search.createFullTextSession( openSession() );
		createIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();

		// Here's how to get a reader from a FullTextSession
		SearchFactory searchFactory = s.getSearchFactory();
		DirectoryProvider provider = searchFactory.getDirectoryProviders( ElectricalProperties.class )[0];
		ReaderProvider readerProvider = searchFactory.getReaderProvider();
		IndexReader reader = readerProvider.openReader( provider );

		/**
		 * Since there are so many combinations of results here, rather
		 * than try to do assertions, this test prints out all the results
		 * found from the three ElectricalProperties entities. This will
		 * do a better  job of demonstrating exactly what the result are. - J.G.
		 */
		///TODO: try and find some ways to assert it. Nobody reads the results. I've added 		
		for (int x = 0; x < 3; x++) {
			TermPositionVector vector = (TermPositionVector) reader.getTermFreqVector( x, "content" );
			assertNotNull( vector );
			String[] terms = vector.getTerms();
			int[] freqs = vector.getTermFrequencies();

			for (int y = 0; y < vector.size(); y++) {
				log.info( "doc# =>" + x );
				log.info( " term => " + terms[y] );
				log.info( " freq => " + freqs[y] );

				int[] positions = vector.getTermPositions( y );
				TermVectorOffsetInfo[] offsets = vector.getOffsets( y );
				for (int z = 0; z < positions.length; z++) {
					log.info( " position => " + positions[z] );
					log.info( " starting offset => " + offsets[z].getStartOffset() );
					log.info( " ending offset => " + offsets[z].getEndOffset() );
				}
				log.info( "---------------" );
			}
		}

		//cleanup
		for (Object element : s.createQuery( "from " + ElectricalProperties.class.getName() ).list())
			s.delete( element );
		tx.commit();
		s.close();
	}

	public void testNoTermVector() throws Exception {
		FullTextSession s = Search.createFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Employee e1 = new Employee( 1000, "Griffin", "ITech" );
		s.save( e1 );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		// Here's how to get a reader from a FullTextSession
		SearchFactory searchFactory = s.getSearchFactory();
		DirectoryProvider provider = searchFactory.getDirectoryProviders( Employee.class )[0];
		ReaderProvider readerProvider = searchFactory.getReaderProvider();
		IndexReader reader = readerProvider.openReader( provider );

		TermPositionVector vector = (TermPositionVector) reader.getTermFreqVector( 0, "dept" );
		assertNull( "should not find a term position vector", vector );

		//cleanup
		for (Object element : s.createQuery( "from " + ElectricalProperties.class.getName() ).list())
			s.delete( element );
		tx.commit();
		s.close();
	}

	private void createIndex(FullTextSession s) {
		Transaction tx = s.beginTransaction();
		ElectricalProperties e1 = new ElectricalProperties( 1000, "Electrical Engineers measure Electrical Properties" );
		s.save( e1 );
		ElectricalProperties e2 = new ElectricalProperties( 1001, "Electrical Properties are interesting" );
		s.save( e2 );
		ElectricalProperties e3 = new ElectricalProperties( 1002, "Electrical Properties are measurable properties" );
		s.save( e3 );

		tx.commit();
	}

	protected Class[] getMappings() {
		return new Class[] {
				ElectricalProperties.class,
				Employee.class
		};
	}
}
