/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.string;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class ISBNValueBridge implements ValueBridge<ISBN, Long> {

	// Implement mandatory toDocumentIdentifier/fromDocumentIdentifier ...
	// ...
	//end::include[]

	@Override
	public Long toIndexedValue(ISBN value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.getId();
	}

	@Override
	public ISBN fromIndexedValue(Long value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : new ISBN( value );
	}

	@Override
	public Long parse(String value) {
		return value == null ? null : Long.parseLong( value.replace( "-", "" ) );
	}

	//tag::include[]

	@Override
	public String format(Long value) { // <1>
		return value == null
				? null
				: value.toString()
						.replaceAll( "(\\d{3})(\\d)(\\d{2})(\\d{6})(\\d)", "$1-$2-$3-$4-$5" );
	}
}
//end::include[]
