//$
package org.hibernate.search.test.filter;

import java.util.BitSet;
import java.io.IOException;

import org.apache.lucene.search.Filter;
import org.apache.lucene.index.IndexReader;

/**
 * @author Emmanuel Bernard
 */
public class ExcludeAllFilter extends Filter {
	private static boolean done = false;

	public BitSet bits(IndexReader reader) throws IOException {
		if (done) throw new IllegalStateException("Called twice");
		BitSet bitSet = new BitSet( reader.maxDoc() );
		done = true;
		return bitSet;
	}
}
