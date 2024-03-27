/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.polymorphism.uninitializedproxy;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
@Cacheable
public class ConcreteEntity extends AbstractEntity {

	@Field
	private String content = "text";

	public ConcreteEntity() {
		super();
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
