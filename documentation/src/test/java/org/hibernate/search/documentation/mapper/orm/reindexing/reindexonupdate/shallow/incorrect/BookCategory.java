/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.shallow.incorrect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

//tag::include[]
@Entity
public class BookCategory {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english")
	private String name;

	// <3>

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	//end::getters-setters[]
}
//end::include[]
