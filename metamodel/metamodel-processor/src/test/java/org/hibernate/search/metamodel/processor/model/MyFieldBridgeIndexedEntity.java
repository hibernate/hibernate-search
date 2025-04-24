/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Indexed
public class MyFieldBridgeIndexedEntity {

	@DocumentId
	private Long id;

	@KeywordField(valueBridge = @ValueBridgeRef(type = MyCustomField.MyCustomFieldBinder.Bridge.class))
	private MyCustomField field;
}
