/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import java.util.Map;

public class LuceneNestedDocumentFieldContribution {

	private final String nestedDocumentPath;
	private final LuceneNestedFieldComparator fieldComparatorSource;

	public LuceneNestedDocumentFieldContribution(String nestedDocumentPath, LuceneNestedFieldComparator fieldComparatorSource) {
		this.nestedDocumentPath = nestedDocumentPath;
		this.fieldComparatorSource = fieldComparatorSource;
	}

	String getNestedDocumentPath() {
		return nestedDocumentPath;
	}

	public void setNestedDocumentMap(Map<Integer, Integer> nestedDocumentMap) {
		fieldComparatorSource.setNestedDocumentMap( nestedDocumentMap );
	}
}
