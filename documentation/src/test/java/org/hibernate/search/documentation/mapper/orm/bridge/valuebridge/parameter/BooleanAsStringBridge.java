/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.parameter;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
class BooleanAsStringBridge implements ValueBridge<Boolean, String> { // <1>

	private final String trueAsString;
	private final String falseAsString;

	BooleanAsStringBridge(String trueAsString, String falseAsString) { // <2>
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
