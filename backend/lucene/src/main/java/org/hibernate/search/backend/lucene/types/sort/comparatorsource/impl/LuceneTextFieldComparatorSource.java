/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.OnTheFlyNestedSorter;
import org.hibernate.search.backend.lucene.types.sort.impl.SortMissingValue;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BytesRef;

public class LuceneTextFieldComparatorSource extends LuceneFieldComparatorSource {

	private final Object missingValue;

	public LuceneTextFieldComparatorSource(String nestedDocumentPath, Object missingValue) {
		super( nestedDocumentPath );
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		final boolean sortMissingLast = missingLast() ^ reversed;

		return new FieldComparator.TermOrdValComparator( numHits, fieldname, sortMissingLast ) {
			@Override
			protected SortedDocValues getSortedDocValues(LeafReaderContext context, String field) throws IOException {
				SortedDocValues sortedDocValues = super.getSortedDocValues( context, field );

				if ( nestedDocsProvider != null ) {
					BitSet parentDocs = nestedDocsProvider.parentDocs( context );
					DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
					if ( parentDocs != null && childDocs != null ) {
						sortedDocValues = OnTheFlyNestedSorter.sort( sortedDocValues, parentDocs, childDocs );
					}
				}

				if ( missingValue == null || missingFirst() || missingLast() ) {
					return sortedDocValues;
				}

				return new LuceneReplaceMissingSortedDocValues( sortedDocValues, (BytesRef) missingValue );
			}
		};
	}

	private boolean missingFirst() {
		return SortMissingValue.MISSING_FIRST.equals( missingValue );
	}

	private boolean missingLast() {
		return SortMissingValue.MISSING_LAST.equals( missingValue );
	}
}
