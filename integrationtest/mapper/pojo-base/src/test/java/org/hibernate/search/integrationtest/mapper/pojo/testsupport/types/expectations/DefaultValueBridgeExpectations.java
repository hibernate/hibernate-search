/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations;

public interface DefaultValueBridgeExpectations<V, F> {
	String TYPE_WITH_VALUE_BRIDGE_1_NAME = "TypeWithValueBridge1Name";
	String TYPE_WITH_VALUE_BRIDGE_2_NAME = "TypeWithValueBridge2Name";
	String TYPE_WITH_VALUE_BRIDGE_FIELD_NAME = "myProperty";
	String TYPE_WITH_VALUE_BRIDGE_FIELD_INDEXNULLAS_NAME = "indexNullAsProperty";

	Class<F> getIndexFieldJavaType();

	Class<?> getTypeWithValueBridge1();

	Object instantiateTypeWithValueBridge1(int identifier, V propertyValue);

	Class<?> getTypeWithValueBridge2();

	F getNullAsValueBridge1();

	F getNullAsValueBridge2();

	default String getUnparsableNullAsValue() {
		return "---";
	}
}
