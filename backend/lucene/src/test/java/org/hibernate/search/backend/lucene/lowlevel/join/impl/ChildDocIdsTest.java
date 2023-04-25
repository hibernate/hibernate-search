/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.join.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.ExplicitDocIdSetIterator;

import org.junit.Test;

import org.apache.lucene.search.ConjunctionUtils;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;

public class ChildDocIdsTest {

	@Test
	public void nextChild_withoutArg() throws IOException {
		ChildDocIds childDocs = new ChildDocIds( bitSet( 2, 7, 10, 14, 20 ), docIdSetIterator( 4, 6, 9, 17, 18 ) );
		assertThat( childDocs.advanceExactParent( 2 ) ).isFalse();

		assertThat( childDocs.advanceExactParent( 7 ) ).isTrue();
		assertThat( childDocs.nextChild() ).isEqualTo( 4 );
		assertThat( childDocs.nextChild() ).isEqualTo( 6 );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 10 ) ).isTrue();
		assertThat( childDocs.nextChild() ).isEqualTo( 9 );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 14 ) ).isFalse();
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 20 ) ).isTrue();
		assertThat( childDocs.nextChild() ).isEqualTo( 17 );
		assertThat( childDocs.nextChild() ).isEqualTo( 18 );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 25 ) ).isFalse();
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
	}

	@Test
	public void nextChild_withArg() throws IOException {
		ChildDocIds childDocs = new ChildDocIds( bitSet( 2, 20 ), docIdSetIterator( 4, 6, 9, 17, 18 ) );
		assertThat( childDocs.advanceExactParent( 2 ) ).isFalse();

		assertThat( childDocs.advanceExactParent( 20 ) ).isTrue();
		assertThat( childDocs.nextChild( 5 ) ).isEqualTo( 6 );
		assertThat( childDocs.nextChild( 8 ) ).isEqualTo( 9 );
		assertThat( childDocs.nextChild( 18 ) ).isEqualTo( 18 );
		assertThat( childDocs.nextChild( 20 ) ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild( 20 ) ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
	}

	@Test
	public void joinedNextChild() throws IOException {
		DocIdSetIterator other = docIdSetIterator( 1, 5, 6, 8, 18, 20, 26, 28, 31, 36 );
		ChildDocIds childDocs = new ChildDocIds( bitSet( 2, 20, 24, 30, 35, 37 ),
				ConjunctionUtils.intersectIterators(
						Arrays.asList( docIdSetIterator( 4, 6, 9, 17, 18, 21, 22, 31, 34, 36 ), other ) ) );
		assertThat( childDocs.advanceExactParent( 2 ) ).isFalse();
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 20 ) ).isTrue();
		assertThat( childDocs.nextChild() ).isEqualTo( 6 );
		assertThat( other.docID() ).isEqualTo( 6 );
		assertThat( childDocs.nextChild() ).isEqualTo( 18 );
		assertThat( other.docID() ).isEqualTo( 18 );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 24 ) ).isFalse();
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 30 ) ).isFalse();
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );

		assertThat( childDocs.advanceExactParent( 37 ) ).isTrue();
		assertThat( childDocs.nextChild() ).isEqualTo( 36 );
		assertThat( other.docID() ).isEqualTo( 36 );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
		assertThat( childDocs.nextChild() ).isEqualTo( DocIdSetIterator.NO_MORE_DOCS );
	}

	private static DocIdSetIterator docIdSetIterator(int... docIds) {
		return ExplicitDocIdSetIterator.of( docIds, 0, Integer.MAX_VALUE );
	}

	private static BitSet bitSet(int... setBits) throws IOException {
		return BitSet.of( docIdSetIterator( setBits ), 1000 );
	}

}
