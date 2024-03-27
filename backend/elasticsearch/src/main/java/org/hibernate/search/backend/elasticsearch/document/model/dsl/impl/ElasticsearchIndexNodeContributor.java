/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;

public interface ElasticsearchIndexNodeContributor {

	void contribute(ElasticsearchIndexNodeCollector collector, ElasticsearchIndexCompositeNode parentNode,
			Map<String, ElasticsearchIndexField> staticChildrenByNameForParent,
			AbstractTypeMapping parentMapping);

}
