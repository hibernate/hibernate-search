/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.missing.impl;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

public class LuceneMissingValueComparatorSource extends FieldComparatorSource {

	private final BytesRef missingValue;

	public LuceneMissingValueComparatorSource(BytesRef missingValue) {
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		return new FieldComparator.TermOrdValComparator( numHits, fieldname, reversed ) {
			@Override
			protected SortedDocValues getSortedDocValues(LeafReaderContext context, String field) throws IOException {
				return new LuceneReplaceMissingSortedDocValues( super.getSortedDocValues( context, field ), missingValue );
			}
		};
	}
}
