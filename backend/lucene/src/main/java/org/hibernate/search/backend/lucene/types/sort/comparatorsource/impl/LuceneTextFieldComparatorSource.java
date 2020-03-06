/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DocValuesJoin;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.ReplaceMissingSortedDocValues;
import org.hibernate.search.backend.lucene.types.sort.impl.SortMissingValue;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;

public class LuceneTextFieldComparatorSource extends LuceneFieldComparatorSource {

	private final Object missingValue;

	public LuceneTextFieldComparatorSource(String nestedDocumentPath, Object missingValue, Query luceneFilter) {
		super( nestedDocumentPath, luceneFilter );
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		final boolean sortMissingLast = missingLast() ^ reversed;

		return new FieldComparator.TermOrdValComparator( numHits, fieldname, sortMissingLast ) {
			@Override
			protected SortedDocValues getSortedDocValues(LeafReaderContext context, String field) throws IOException {
				SortedDocValues sortedDocValues = DocValuesJoin.getJoinedAsSingleValuedSorted( context, field, nestedDocsProvider );

				if ( missingValue == null || missingFirst() || missingLast() ) {
					return sortedDocValues;
				}

				return new ReplaceMissingSortedDocValues( sortedDocValues, (BytesRef) missingValue );
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
