/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.ormcontext;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class MyEntity {

	@Id
	@DocumentId(identifierBridge = @IdentifierBridgeRef(type = MyDataIdentifierBridge.class))
	private MyData id;

	public MyData getId() {
		return id;
	}

	public void setId(MyData id) {
		this.id = id;
	}
}
