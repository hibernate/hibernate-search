/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * The low-level provider of {@link ElasticsearchIndexFieldTypeFactory}.
 * <p>
 * Different implementations of this factory may exist for different versions of Elasticsearch.
 */
public interface ElasticsearchIndexFieldTypeFactoryProvider {

	ElasticsearchIndexFieldTypeFactory create(EventContext eventContext, BackendMapperContext backendMapperContext,
			IndexFieldTypeDefaultsProvider defaultsProvider);

}
