/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.TermVector;

/**
 * @author Emmanuel Bernard
 */
class CoffeeBrand {

	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private Integer id;

	@Field(termVector = TermVector.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@Field(termVector = TermVector.NO)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String description;
}
