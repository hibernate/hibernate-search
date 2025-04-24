/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Indexed
public class MyIdBridgeIndexedEntity {

	@DocumentId(
			identifierBridge = @IdentifierBridgeRef(type = MyCustomId.MyCustomIdBinder.Bridge.class)
	)
	private MyCustomId id;

	@KeywordField
	private String string;
}
