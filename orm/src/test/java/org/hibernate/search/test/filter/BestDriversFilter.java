/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;

/**
 * @author Emmanuel Bernard
 */
public class BestDriversFilter extends Filter {

	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		AtomicReader reader = context.reader();
		OpenBitSet bitSet = new OpenBitSet( reader.maxDoc() );
		DocsEnum termDocsEnum = reader.termDocsEnum( new Term( "score", "5" ) );
		if ( termDocsEnum == null ) {
			return bitSet; // All bits already correctly set
		}
		while ( termDocsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS ) {
			final int docID = termDocsEnum.docID();
			if ( acceptDocs == null || acceptDocs.get( docID ) ) {
				bitSet.set( docID );
			}
		}
		return bitSet;
	}

}
