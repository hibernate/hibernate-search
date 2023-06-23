/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
