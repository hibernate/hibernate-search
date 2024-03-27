/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.PropertyMappingIndexSettingsContributor;

public interface ElasticsearchIndexNodeCollector {

	void collect(String absolutePath, ElasticsearchIndexObjectField node);

	void collect(String absoluteFieldPath, ElasticsearchIndexValueField<?> node);

	void collect(ElasticsearchIndexObjectFieldTemplate template);

	void collect(ElasticsearchIndexValueFieldTemplate template);

	void collect(NamedDynamicTemplate templateForMapping);

	PropertyMappingIndexSettingsContributor propertyMappingIndexSettingsContributor();

}
