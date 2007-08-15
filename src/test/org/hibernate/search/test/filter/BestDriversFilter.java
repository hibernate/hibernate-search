//$Id$
package org.hibernate.search.test.filter;

import java.util.BitSet;
import java.io.IOException;

import org.apache.lucene.search.Filter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;

/**
 * @author Emmanuel Bernard
 */
public class BestDriversFilter extends Filter {

	public BitSet bits(IndexReader reader) throws IOException {
		BitSet bitSet = new BitSet( reader.maxDoc() );
		TermDocs termDocs = reader.termDocs( new Term("score", "5") );
		while ( termDocs.next() ) {
			bitSet.set( termDocs.doc() );
		}
		return bitSet;
	}
}
