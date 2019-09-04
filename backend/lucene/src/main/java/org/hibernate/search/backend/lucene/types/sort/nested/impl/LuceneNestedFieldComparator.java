/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import java.util.Map;

import org.apache.lucene.search.FieldComparatorSource;

public abstract class LuceneNestedFieldComparator extends FieldComparatorSource {

	protected Map<Integer, Integer> nestedDocumentMap;

	public void setNestedDocumentMap(Map<Integer, Integer> nestedDocumentMap) {
		this.nestedDocumentMap = nestedDocumentMap;
	}

	public Integer getNestedDocument(Integer rootDocument) {
		// on the first call the nestedDocumentMap is not set
		if ( nestedDocumentMap == null ) {
			return rootDocument;
		}

		// it is possible that a document does not have a value set for the nested field
		Integer nestedDoc = nestedDocumentMap.get( rootDocument );
		return ( nestedDoc == null ) ? rootDocument : nestedDoc;
	}
}
