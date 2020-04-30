/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;

import com.google.gson.JsonElement;

public class IndexAliasDefinitionValidator implements Validator<IndexAliasDefinition> {

	private final Validator<JsonElement> extraAttributeValidator = new JsonElementValidator( new JsonElementEquivalence() );

	@Override
	public void validate(ValidationErrorCollector errorCollector, IndexAliasDefinition expectedDefinition,
			IndexAliasDefinition actualDefinition) {
		if ( Boolean.TRUE.equals( expectedDefinition.getWriteIndex() ) ) {
			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.ALIAS_ATTRIBUTE, "is_write_index",
					Boolean.TRUE, actualDefinition.getWriteIndex()
			);
		}

		extraAttributeValidator.validateAllIncludingUnexpected(
				errorCollector, ValidationContextType.ALIAS_ATTRIBUTE,
				expectedDefinition.getExtraAttributes(), actualDefinition.getExtraAttributes()
		);
	}

}
