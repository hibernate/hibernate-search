/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueFieldTemplate;

public interface LuceneIndexNodeCollector {
	void collect(String absolutePath, LuceneIndexObjectField node);

	void collect(String absoluteFieldPath, LuceneIndexValueField<?> node);

	void collect(LuceneIndexObjectFieldTemplate template);

	void collect(LuceneIndexValueFieldTemplate template);
}
