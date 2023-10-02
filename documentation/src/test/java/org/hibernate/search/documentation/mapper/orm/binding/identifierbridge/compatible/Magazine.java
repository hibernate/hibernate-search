/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.compatible;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Magazine {

	@EmbeddedId
	@DocumentId(
			identifierBridge = @IdentifierBridgeRef(type = BookOrMagazineIdBridge.class)
	)
	private BookOrMagazineId id = new BookOrMagazineId();

	@FullTextField(analyzer = "english")
	private String title;

	public BookOrMagazineId getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
