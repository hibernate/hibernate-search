/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.onthefly.impl;

import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Query;

public abstract class NestedFieldComparatorSource extends FieldComparatorSource {

	private final String nestedDocumentPath;

	protected ParentChildDocsProvider docsProvider;

	public NestedFieldComparatorSource(String nestedDocumentPath) {
		this.nestedDocumentPath = nestedDocumentPath;
	}

	public String getNestedDocumentPath() {
		return nestedDocumentPath;
	}

	public void setOriginalParentQuery(Query luceneQuery) {
		this.docsProvider = new ParentChildDocsProvider( nestedDocumentPath, luceneQuery );
	}
}
