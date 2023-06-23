/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonElement;

public interface UnknownTypeJsonAccessor extends JsonAccessor<JsonElement> {

	JsonObjectAccessor asObject();

	JsonArrayAccessor asArray();

	JsonAccessor<String> asString();

	JsonAccessor<Boolean> asBoolean();

	JsonAccessor<Integer> asInteger();

	JsonAccessor<Long> asLong();

	JsonAccessor<Float> asFloat();

	JsonAccessor<Double> asDouble();

	UnknownTypeJsonAccessor element(int index);

	UnknownTypeJsonAccessor property(String propertyName);

}
