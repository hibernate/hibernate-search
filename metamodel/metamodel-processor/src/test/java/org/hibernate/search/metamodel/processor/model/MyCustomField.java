/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public record MyCustomField(long id, String string) {

	public class MyCustomFieldBinder implements ValueBinder {

		@Override
		public void bind(ValueBindingContext<?> context) {
			context.bridge(
					MyCustomField.class,
					new Bridge()
			);
		}

		public static class Bridge implements ValueBridge<MyCustomField, String> {

			@Override
			public String toIndexedValue(MyCustomField value, ValueBridgeToIndexedValueContext context) {
				return value.id() + "/" + value.string();
			}

			@Override
			public MyCustomField fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
				if ( value == null ) {
					return null;
				}
				String[] split = value.split( "/" );
				return new MyCustomField( Long.parseLong( split[0] ), split[1] );
			}
		}
	}

}
