/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Indexed
public class MyIdBinderIndexedEntity {

	@DocumentId(
			identifierBinder = @IdentifierBinderRef(type = MyCustomId.MyCustomIdBinder.class)
	)
	private MyCustomId id;

	@KeywordField
	private String string;


}
