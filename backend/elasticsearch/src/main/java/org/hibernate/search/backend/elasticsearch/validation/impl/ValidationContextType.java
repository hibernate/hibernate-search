/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

public enum ValidationContextType {

	ALIAS,
	ALIAS_ATTRIBUTE,
	MAPPING_PROPERTY,
	MAPPING_ATTRIBUTE,
	ANALYZER,
	NORMALIZER,
	CHAR_FILTER,
	TOKENIZER,
	TOKEN_FILTER,
	ANALYSIS_DEFINITION_PARAMETER,
	DYNAMIC_TEMPLATE,
	DYNAMIC_TEMPLATE_ATTRIBUTE,
	CUSTOM_INDEX_SETTINGS_ATTRIBUTE,
	CUSTOM_INDEX_MAPPING_ATTRIBUTE

}
