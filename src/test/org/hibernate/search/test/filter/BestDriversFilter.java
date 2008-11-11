//$Id$
package org.hibernate.search.test.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

/**
 * @author Emmanuel Bernard
 */
public class BestDriversFilter extends Filter {

	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		OpenBitSet bitSet = new OpenBitSet( reader.maxDoc() );
		TermDocs termDocs = reader.termDocs( new Term( "score", "5" ) );
		while ( termDocs.next() ) {
			bitSet.set( termDocs.doc() );
		}
		return bitSet;
	}
}
