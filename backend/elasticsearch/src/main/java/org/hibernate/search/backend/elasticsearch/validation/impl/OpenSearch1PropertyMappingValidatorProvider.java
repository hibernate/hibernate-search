/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;

public class OpenSearch1PropertyMappingValidatorProvider implements ElasticsearchPropertyMappingValidatorProvider {
	@Override
	public Validator<PropertyMapping> create() {
		return new PropertyMappingValidator.OpenSearch1PropertyMappingValidator();
	}
}
