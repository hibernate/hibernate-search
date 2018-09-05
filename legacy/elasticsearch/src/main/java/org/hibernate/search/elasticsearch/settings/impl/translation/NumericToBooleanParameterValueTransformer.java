/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

class NumericToBooleanParameterValueTransformer implements ParameterValueTransformer {
	static final NumericToBooleanParameterValueTransformer INSTANCE = new NumericToBooleanParameterValueTransformer();

	private NumericToBooleanParameterValueTransformer() {
	}

	@Override
	public JsonElement transform(String parameterValue) {
		/*
		 * Mirror the behavior of WordDelimiterFilterFactory:
		 * 0 is false and anything else is true.
		 */
		int integerValue = Integer.parseInt( parameterValue );
		return new JsonPrimitive( integerValue != 0 );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ".INSTANCE";
	}
}