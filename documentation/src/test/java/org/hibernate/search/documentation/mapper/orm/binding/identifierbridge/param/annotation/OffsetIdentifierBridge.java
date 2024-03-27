/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.param.annotation;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

//tag::include[]
public class OffsetIdentifierBridge implements IdentifierBridge<Integer> { // <1>

	private final int offset;

	public OffsetIdentifierBridge(int offset) { // <2>
		this.offset = offset;
	}

	@Override
	public String toDocumentIdentifier(Integer propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
		return String.valueOf( propertyValue + offset );
	}

	@Override
	public Integer fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		return Integer.parseInt( documentIdentifier ) - offset;
	}
}
//end::include[]
