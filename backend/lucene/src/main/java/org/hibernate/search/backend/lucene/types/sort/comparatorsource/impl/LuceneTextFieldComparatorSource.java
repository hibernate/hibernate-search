/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.ReplaceMissingSortedDocValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.types.sort.impl.SortMissingValue;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;

public class LuceneTextFieldComparatorSource extends LuceneFieldComparatorSource {

	private final Object missingValue;
	private final MultiValueMode multiValueMode;

	public LuceneTextFieldComparatorSource(String nestedDocumentPath, Object missingValue, MultiValueMode multiValueMode, Query luceneFilter) {
		super( nestedDocumentPath, luceneFilter );
		this.missingValue = missingValue;
		this.multiValueMode = multiValueMode;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		final boolean considerMissingHighest;
		if ( SortMissingValue.MISSING_LOWEST.equals( missingValue ) ) {
			considerMissingHighest = false;
		}
		else if ( SortMissingValue.MISSING_HIGHEST.equals( missingValue ) ) {
			considerMissingHighest = true;
		}
		else if ( SortMissingValue.MISSING_LAST.equals( missingValue ) ) {
			// To appear last, missing values must be considered highest, or lowest if the order is reversed.
			considerMissingHighest = !reversed;
		}
		else { // SortMissingValue.MISSING_FIRST, the default
			// To appear first, missing values must be considered lowest, or highest if the order is reversed.
			considerMissingHighest = reversed;
		}
		TextMultiValuesToSingleValuesSource source =
				TextMultiValuesToSingleValuesSource.fromField( fieldname, multiValueMode, nestedDocsProvider );

		return new FieldComparator.TermOrdValComparator( numHits, fieldname, considerMissingHighest ) {
			@Override
			protected SortedDocValues getSortedDocValues(LeafReaderContext context, String field) throws IOException {
				SortedDocValues sortedDocValues = source.getValues( context );

				if ( missingValue == null || isOneOfSortMissingValues() ) {
					return sortedDocValues;
				}

				return new ReplaceMissingSortedDocValues( sortedDocValues, (BytesRef) missingValue );
			}
		};
	}

	private boolean isOneOfSortMissingValues() {
		return missingValue instanceof SortMissingValue;
	}
}
