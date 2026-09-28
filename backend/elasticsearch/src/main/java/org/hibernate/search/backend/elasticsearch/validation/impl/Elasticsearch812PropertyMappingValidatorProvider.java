package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;

public class Elasticsearch812PropertyMappingValidatorProvider implements ElasticsearchPropertyMappingValidatorProvider {
	@Override
	public Validator<PropertyMapping> create() {
		return new PropertyMappingValidator.Elasticsearch812PropertyMappingValidator();
	}
}
