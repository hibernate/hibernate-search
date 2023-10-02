/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

class MyProductIdBridge implements IdentifierBridge<MyProductId> {
	@Override
	public String toDocumentIdentifier(MyProductId value, IdentifierBridgeToDocumentIdentifierContext context) {
		return value.getProducerId() + "/" + value.getProducerProductId();
	}

	@Override
	public MyProductId fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		String[] split = documentIdentifier.split( "/" );
		return new MyProductId( split[0], split[1] );
	}
}
