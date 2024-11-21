/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Query;

public abstract class LuceneFieldComparatorSource extends FieldComparatorSource {

	protected NestedDocsProvider nestedDocsProvider;

	public LuceneFieldComparatorSource(String nestedDocumentPath, Query filter) {
		this.nestedDocsProvider = nestedDocumentPath == null
				? null
				: new NestedDocsProvider( nestedDocumentPath, filter );
	}

}
