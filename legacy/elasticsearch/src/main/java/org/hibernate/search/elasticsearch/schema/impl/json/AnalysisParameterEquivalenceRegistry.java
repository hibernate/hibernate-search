/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.json;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the knowledge of how to compare values for a given parameter
 * for a given analysis type (analyzer type, char filter type, etc.).
 *
 * @author Yoann Rodiere
 */
public class AnalysisParameterEquivalenceRegistry {

	private static final AnalysisJsonElementEquivalence DEFAULT_ELEMENT_EQUIVALENCE =
			new AnalysisJsonElementEquivalence();

	// Nested arrays are not considered unordered.
	private static final AnalysisJsonElementEquivalence UNORDERED_ARRAY_EQUIVALENCE =
			new AnalysisJsonElementUnorderedArrayEquivalence( DEFAULT_ELEMENT_EQUIVALENCE );

	private final Map<String, Map<String, AnalysisJsonElementEquivalence>> equivalences;

	private AnalysisParameterEquivalenceRegistry(
			Map<String, Map<String, AnalysisJsonElementEquivalence>> equivalences) {
		super();
		this.equivalences = equivalences;
	}

	public AnalysisJsonElementEquivalence get(String type, String parameter) {
		Map<String, AnalysisJsonElementEquivalence> mapForType = equivalences.get( type );
		AnalysisJsonElementEquivalence result = mapForType == null ? null : mapForType.get( parameter );
		return result == null ? DEFAULT_ELEMENT_EQUIVALENCE : result;
	}

	public static class Builder {

		private final Map<String, Map<String, AnalysisJsonElementEquivalence>> equivalences = new HashMap<>();

		public TypeBuilder type(String name) {
			Map<String, AnalysisJsonElementEquivalence> mapForType = equivalences.get( name );
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
		private Map<String, AnalysisJsonElementEquivalence> equivalences;

		private TypeBuilder(Builder parent, Map<String, AnalysisJsonElementEquivalence> equivalences) {
			super();
			this.parent = parent;
			this.equivalences = equivalences;
		}

		public ParameterBuilder param(String name) {
			return new ParameterBuilder( this, name );
		}

		private void add(String parameterName, AnalysisJsonElementEquivalence equivalence) {
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
