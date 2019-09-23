/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.binder;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class ISBNValueBinder implements ValueBinder { // <1>
	@Override
	public void bind(ValueBindingContext<?> context) { // <2>
		context.setBridge( // <3>
				ISBN.class, // <4>
				new ISBNValueBridge(), // <5>
				context.getTypeFactory() // <6>
						.asString() // <7>
						.normalizer( "isbn" ) // <8>
		);
	}

	private static class ISBNValueBridge implements ValueBridge<ISBN, String> {
		@Override
		public String toIndexedValue(ISBN value, ValueBridgeToIndexedValueContext context) { // <9>
			return value == null ? null : value.getStringValue();
		}
	}
}
//end::include[]
