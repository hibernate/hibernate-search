/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface LuceneSearchIndexScope<S extends LuceneSearchIndexScope<?>>
		extends SearchIndexScope<S> {

	@Override
	LuceneSearchIndexNodeContext child(SearchIndexCompositeNodeContext<?> parent, String name);

	LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry();

}
