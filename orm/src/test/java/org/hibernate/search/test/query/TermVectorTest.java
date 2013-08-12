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
package org.hibernate.search.test.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author John Griffin
 */
public class TermVectorTest extends SearchTestCase {

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
		TermPositionVector vector = (TermPositionVector) reader.getTermFreqVector( x, "content" );
		assertNotNull( vector );
		String[] terms = vector.getTerms();
		int[] freqs = vector.getTermFrequencies();

		assertEquals( "electrical", terms[x] );
		assertEquals( 2, freqs[x] );

		TermVectorOffsetInfo[] offsets = vector.getOffsets( x );
		assertEquals( 0, offsets[x].getStartOffset() );
		assertEquals( 10, offsets[x].getEndOffset() );

		int[] termPositions = vector.getTermPositions( 0 );
		assertEquals( 0, termPositions[0] );
		assertEquals( 3, termPositions[1] );

		// cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		searchFactory.getIndexReaderAccessor().close( reader );
		tx.commit();
		s.close();
	}

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

		TermPositionVector vector = (TermPositionVector) reader.getTermFreqVector( 0, "dept" );
		assertNull( "should not find a term position vector", vector );

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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ElectricalProperties.class, Employee.class };
	}
}
