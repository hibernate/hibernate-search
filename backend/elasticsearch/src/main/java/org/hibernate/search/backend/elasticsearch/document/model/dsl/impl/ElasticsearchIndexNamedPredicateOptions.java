/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;

public record ElasticsearchIndexNamedPredicateOptions<T>(TreeNodeInclusion inclusion, T definition)
		implements IndexSchemaNamedPredicateOptionsStep {

}
