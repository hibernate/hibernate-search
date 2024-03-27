/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.binder;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class ISBNValueBinder implements ValueBinder { // <1>
	@Override
	public void bind(ValueBindingContext<?> context) { // <2>
		context.bridge( // <3>
				ISBN.class, // <4>
				new ISBNValueBridge(), // <5>
				context.typeFactory() // <6>
						.asString() // <7>
						.normalizer( "isbn" ) // <8>
		);
	}

	private static class ISBNValueBridge implements ValueBridge<ISBN, String> {
		@Override
		public String toIndexedValue(ISBN value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.getStringValue(); // <9>
		}
	}
}
//end::include[]
