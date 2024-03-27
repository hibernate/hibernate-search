/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.document.model.spi.IndexField;

public interface LuceneIndexField
		extends IndexField<LuceneSearchIndexScope<?>, LuceneIndexCompositeNode>, LuceneIndexNode {
}
