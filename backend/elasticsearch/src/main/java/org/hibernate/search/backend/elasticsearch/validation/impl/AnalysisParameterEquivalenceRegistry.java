/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the knowledge of how to compare values for a given parameter
 * for a given analysis type (analyzer type, char filter type, etc.).
 *
 */
class AnalysisParameterEquivalenceRegistry {

	private static final JsonElementEquivalence DEFAULT_ELEMENT_EQUIVALENCE =
			new JsonElementEquivalence();

	// Nested arrays are not considered unordered.
	private static final JsonElementEquivalence UNORDERED_ARRAY_EQUIVALENCE =
			new JsonElementUnorderedArrayEquivalence( DEFAULT_ELEMENT_EQUIVALENCE );

	private final Map<String, Map<String, JsonElementEquivalence>> equivalences;

	private AnalysisParameterEquivalenceRegistry(
			Map<String, Map<String, JsonElementEquivalence>> equivalences) {
		super();
		this.equivalences = equivalences;
	}

	public JsonElementEquivalence get(String type, String parameter) {
		Map<String, JsonElementEquivalence> mapForType = equivalences.get( type );
		JsonElementEquivalence result = mapForType == null ? null : mapForType.get( parameter );
		return result == null ? DEFAULT_ELEMENT_EQUIVALENCE : result;
	}

	public static class Builder {

		private final Map<String, Map<String, JsonElementEquivalence>> equivalences = new HashMap<>();

		public TypeBuilder type(String name) {
			Map<String, JsonElementEquivalence> mapForType = equivalences.get( name );
			if ( mapForType == null ) {
				mapForType = new HashMap<>();
				equivalences.put( name, mapForType );
			}
			return new TypeBuilder( this, mapForType );
		}

		public AnalysisParameterEquivalenceRegistry build() {
			return new AnalysisParameterEquivalenceRegistry( equivalences );
		}
	}

	public static class TypeBuilder {

		private final Builder parent;
		private final Map<String, JsonElementEquivalence> equivalences;

		private TypeBuilder(Builder parent, Map<String, JsonElementEquivalence> equivalences) {
			super();
			this.parent = parent;
			this.equivalences = equivalences;
		}

		public ParameterBuilder param(String name) {
			return new ParameterBuilder( this, name );
		}

		private void add(String parameterName, JsonElementEquivalence equivalence) {
			equivalences.put( parameterName, equivalence );
		}

		public Builder end() {
			return parent;
		}
	}

	public static class ParameterBuilder {

		private final TypeBuilder parent;
		private final String parameterName;

		private ParameterBuilder(TypeBuilder parent, String parameterName) {
			super();
			this.parent = parent;
			this.parameterName = parameterName;
		}

		public TypeBuilder unorderedArray() {
			parent.add( parameterName, UNORDERED_ARRAY_EQUIVALENCE );
			return parent;
		}
	}
}
