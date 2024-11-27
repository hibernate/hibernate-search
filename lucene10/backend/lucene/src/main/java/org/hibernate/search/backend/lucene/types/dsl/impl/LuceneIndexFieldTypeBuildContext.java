/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.util.common.reporting.EventContext;

public interface LuceneIndexFieldTypeBuildContext {

	EventContext getEventContext();

	LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry();

	BackendMappingHints hints();

}
