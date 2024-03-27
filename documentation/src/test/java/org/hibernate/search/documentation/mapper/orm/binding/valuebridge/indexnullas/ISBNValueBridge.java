/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.indexnullas;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class ISBNValueBridge implements ValueBridge<ISBN, String> {

	@Override
	public String toIndexedValue(ISBN value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.getStringValue();
	}

	@Override
	public String parse(String value) {
		// Just check the string format and return the string
		return ISBN.parse( value ).getStringValue(); // <1>
	}
}
//end::include[]
