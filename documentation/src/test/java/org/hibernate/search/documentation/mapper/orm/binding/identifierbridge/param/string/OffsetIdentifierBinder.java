/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.param.string;

import org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.param.annotation.OffsetIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;

//tag::include[]
public class OffsetIdentifierBinder implements IdentifierBinder {

	@Override
	public void bind(IdentifierBindingContext<?> context) {
		String offset = context.param( "offset", String.class ); // <1>
		context.bridge(
				Integer.class,
				new OffsetIdentifierBridge( Integer.parseInt( offset ) ) // <2>
		);
	}
}
//end::include[]
