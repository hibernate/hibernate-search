/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;

public interface LuceneSearchProjectionIndexScope<S extends LuceneSearchProjectionIndexScope<?>>
		extends SearchProjectionIndexScope<S>, LuceneSearchIndexScope<S> {

	@Override
	LuceneSearchProjectionBuilderFactory projectionBuilders();

}
