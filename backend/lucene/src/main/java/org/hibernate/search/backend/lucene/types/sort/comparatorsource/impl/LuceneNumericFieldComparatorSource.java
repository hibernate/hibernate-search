/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.search.FieldComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;

public class LuceneNumericFieldComparatorSource<E extends Number> extends LuceneFieldComparatorSource {

	private final E missingValue;
	private final LuceneNumericDomain<E> numericDomain;
	private final MultiValueMode sortMode;

	public LuceneNumericFieldComparatorSource(String nestedDocumentPath, LuceneNumericDomain<E> numericDomain, E missingValue, MultiValueMode sortMode) {
		super( nestedDocumentPath );
		this.numericDomain = numericDomain;
		this.missingValue = missingValue;
		this.sortMode = sortMode;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		return numericDomain.createFieldComparator( fieldname, numHits, sortMode, missingValue, nestedDocsProvider );
	}
}
