package org.hibernate.search.documentation.backend.elasticsearch.type.asnative;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

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
						.asNative( JacksonJsonHandlers.value() ) // <4>
								.mapping( "{\"type\": \"ip\"}" ) // <5>
		);
	}

	private static class IpAddressValueBridge implements ValueBridge<String, JsonNode> {
		@Override
		public JsonNode toIndexedValue(String value,
				ValueBridgeToIndexedValueContext context) {
			return value == null ? null : StringNode.valueOf( value ); // <6>
		}

		@Override
		public String fromIndexedValue(JsonNode value,
				ValueBridgeFromIndexedValueContext context) {
			return value == null ? null : value.asString(); // <7>
		}
	}
}
//end::include[]
// @formatter:on
