/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

import com.google.gson.JsonElement;

class JsonElementValidator implements Validator<JsonElement> {

	private final JsonElementEquivalenceProvider equivalenceProvider;

	JsonElementValidator(JsonElementEquivalence equivalence) {
		this( ignored -> equivalence );
	}

	JsonElementValidator(JsonElementEquivalenceProvider equivalenceProvider) {
		this.equivalenceProvider = equivalenceProvider;
	}

	@Override
	public void validate(ValidationErrorCollector theErrorCollector, JsonElement expected, JsonElement actual) {
		JsonElementEquivalence parameterEquivalence = equivalenceProvider.get( theErrorCollector.getCurrentName() );
		if ( !parameterEquivalence.isEquivalent( expected, actual ) ) {
			theErrorCollector.addError(
					ElasticsearchValidationMessages.INSTANCE.invalidValue( expected, actual )
			);
		}
	}

	public interface JsonElementEquivalenceProvider {
		JsonElementEquivalence get(String elementName);
	}
}
