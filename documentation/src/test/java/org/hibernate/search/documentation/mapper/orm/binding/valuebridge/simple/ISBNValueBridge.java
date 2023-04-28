/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.simple;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class ISBNValueBridge implements ValueBridge<ISBN, String> { // <1>

	@Override
	public String toIndexedValue(ISBN value, ValueBridgeToIndexedValueContext context) { // <2>
		return value == null ? null : value.getStringValue();
	}

}
//end::include[]
