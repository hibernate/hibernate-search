/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;

public class NestedNumericFieldComparatorSource<E extends Number> extends NestedFieldComparatorSource {

	private final E missingValue;
	private final LuceneNumericDomain<E> numericDomain;

	private LuceneNumericDomain.NumericNestedFieldComparator<E> fieldComparator;

	public NestedNumericFieldComparatorSource(String nestedDocumentPath, LuceneNumericDomain<E> numericDomain, E missingValue) {
		super( nestedDocumentPath );
		this.numericDomain = numericDomain;
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		fieldComparator = numericDomain.createNestedFieldComparator( fieldname, numHits, missingValue );

		// workaround since we cannot extend a FieldComparator
		return fieldComparator.getComparator();
	}

	@Override
	public void setOriginalParentQuery(Query luceneQuery) {
		super.setOriginalParentQuery( luceneQuery );
		fieldComparator.setNestedDocsProvider( nestedDocsProvider );
	}
}
