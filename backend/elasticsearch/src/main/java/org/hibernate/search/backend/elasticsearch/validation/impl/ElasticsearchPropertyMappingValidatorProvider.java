package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;

public interface ElasticsearchPropertyMappingValidatorProvider {
	Validator<PropertyMapping> create();
}
