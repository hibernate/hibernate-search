/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.FieldComparatorSource;

public abstract class LuceneNestedFieldComparator extends FieldComparatorSource {

	protected Map<Integer, Set<Integer>> nestedDocumentMap;

	public void setNestedDocumentMap(Map<Integer, Set<Integer>> nestedDocumentMap) {
		this.nestedDocumentMap = nestedDocumentMap;
	}

	public Integer getNestedDocument(Integer rootDocument) {
		// on the first call the nestedDocumentMap is not set
		if ( nestedDocumentMap == null ) {
			return rootDocument;
		}

		// it is possible that a document does not have a value set for the nested field
		Set<Integer> nestedDocs = nestedDocumentMap.get( rootDocument );
		if ( nestedDocs == null ) {
			return rootDocument;
		}

		// TODO at the moment we do not support multi value sorting on nested document
		// using one of them in case
		return nestedDocs.iterator().next();
	}
}
