/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.type.asnative;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

// @formatter:off
//tag::include[]
public class IpAddressValueBinder implements ValueBinder { // <1>
	@Override
	public void bind(ValueBindingContext<?> context) {
		context.bridge(
				String.class,
				new IpAddressValueBridge(),
				context.typeFactory() // <2>
						.extension( ElasticsearchExtension.get() ) // <3>
						.asNative() // <4>
								.mapping( "{\"type\": \"ip\"}" ) // <5>
		);
	}

	private static class IpAddressValueBridge implements ValueBridge<String, JsonElement> {
		@Override
		public JsonElement toIndexedValue(String value,
				ValueBridgeToIndexedValueContext context) {
			return value == null ? null : new JsonPrimitive( value ); // <6>
		}

		@Override
		public String fromIndexedValue(JsonElement value,
				ValueBridgeFromIndexedValueContext context) {
			return value == null ? null : value.getAsString(); // <7>
		}
	}
}
//end::include[]
// @formatter:on
