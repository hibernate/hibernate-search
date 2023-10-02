/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.util.Map;

import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;

/**
 * Information about a composite index element targeted by search; either the index root or an object field.
 * <p>
 * For now this is only used in predicates.
 */
public interface LuceneSearchIndexCompositeNodeContext
		extends SearchIndexCompositeNodeContext<LuceneSearchIndexScope<?>>,
		LuceneSearchIndexNodeContext {

	@Override
	LuceneSearchIndexCompositeNodeTypeContext type();

	@Override
	Map<String, ? extends LuceneSearchIndexNodeContext> staticChildrenByName();

}
