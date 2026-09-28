package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;

public class OpenSearch2PropertyMappingValidatorProvider implements ElasticsearchPropertyMappingValidatorProvider {
	@Override
	public Validator<PropertyMapping> create() {
		return new PropertyMappingValidator.OpenSearch2PropertyMappingValidator();
	}
}
