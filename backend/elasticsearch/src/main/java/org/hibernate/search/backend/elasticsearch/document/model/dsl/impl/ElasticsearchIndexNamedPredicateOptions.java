/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;

public class ElasticsearchIndexNamedPredicateOptions implements IndexSchemaNamedPredicateOptionsStep {

	public final TreeNodeInclusion inclusion;
	public final PredicateDefinition definition;

	ElasticsearchIndexNamedPredicateOptions(TreeNodeInclusion inclusion, PredicateDefinition definition) {
		this.inclusion = inclusion;
		this.definition = definition;
	}

}
