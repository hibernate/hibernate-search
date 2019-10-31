/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.type.asnative;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import com.google.gson.Gson;

//tag::include[]
public class IpAddressValueBinder implements ValueBinder { // <1>
	@Override
	public void bind(ValueBindingContext<?> context) {
		context.setBridge(
				String.class,
				new IpAddressValueBridge(),
				context.getTypeFactory() // <2>
						.extension( ElasticsearchExtension.get() ) // <3>
						.asNative( // <4>
								"{\"type\": \"ip\"}" // <5>
						)
		);
	}

	private static class IpAddressValueBridge implements ValueBridge<String, String> {
		private static final Gson GSON = new Gson();

		@Override
		public String toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : GSON.toJson( value ); // <6>
		}

		@Override
		public String fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
			return value == null ? null : GSON.fromJson( value, String.class ); // <7>
		}
	}
}
//end::include[]
