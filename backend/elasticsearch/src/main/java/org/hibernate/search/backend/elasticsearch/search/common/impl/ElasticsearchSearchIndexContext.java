/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;

/**
 * Information about an index targeted by search,
 * be it in a projection, a predicate, a sort, ...
 */
public interface ElasticsearchSearchIndexContext {

	IndexNames names();

	String mappedTypeName();

	SearchIndexIdentifierContext identifier();

	int maxResultWindow();

}
