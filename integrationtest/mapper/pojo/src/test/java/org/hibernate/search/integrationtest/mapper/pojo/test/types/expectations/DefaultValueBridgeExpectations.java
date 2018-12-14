/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations;

import java.util.List;

public interface DefaultValueBridgeExpectations<V, F> {
	String TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME = "TypeWithValueBridge1IndexName";
	String TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME = "TypeWithValueBridge2IndexName";
	String TYPE_WITH_VALUE_BRIDGE_FIELD_NAME = "myProperty";

	Class<V> getProjectionType();

	Class<F> getIndexFieldJavaType();

	List<V> getEntityPropertyValues();

	default boolean isNullTranslatedAsNull() {
		return true;
	}

	List<F> getDocumentFieldValues();

	Class<?> getTypeWithValueBridge1();

	Object instantiateTypeWithValueBridge1(int identifier, V propertyValue);

	Class<?> getTypeWithValueBridge2();
}
