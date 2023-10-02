/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.param.annotation;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class BooleanAsStringBridge implements ValueBridge<Boolean, String> { // <1>

	private final String trueAsString;
	private final String falseAsString;

	public BooleanAsStringBridge(String trueAsString, String falseAsString) { // <2>
		this.trueAsString = trueAsString;
		this.falseAsString = falseAsString;
	}

	@Override
	public String toIndexedValue(Boolean value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		return value ? trueAsString : falseAsString;
	}
}
//end::include[]
