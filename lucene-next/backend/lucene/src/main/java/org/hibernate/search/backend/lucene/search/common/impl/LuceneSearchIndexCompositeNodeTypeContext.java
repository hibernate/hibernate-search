/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeTypeContext;

public interface LuceneSearchIndexCompositeNodeTypeContext
		extends SearchIndexCompositeNodeTypeContext<LuceneSearchIndexScope<?>, LuceneSearchIndexCompositeNodeContext> {

}
