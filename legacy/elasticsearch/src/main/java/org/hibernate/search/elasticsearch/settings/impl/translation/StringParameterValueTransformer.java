/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

class StringParameterValueTransformer implements ParameterValueTransformer {
	static final StringParameterValueTransformer INSTANCE = new StringParameterValueTransformer();

	private StringParameterValueTransformer() {
	}

	@Override
	public JsonElement transform(String parameterValue) {
		return new JsonPrimitive( parameterValue );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ".INSTANCE";
	}
}