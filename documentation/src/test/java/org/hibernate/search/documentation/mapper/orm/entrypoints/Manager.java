/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.entrypoints;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Manager implements Person {

	@Id
	private Integer id;

	@FullTextField(analyzer = "name", projectable = Projectable.YES)
	private String name;

	@OneToMany
	@OrderColumn
	private List<Associate> associates = new ArrayList<>();

	public Manager() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Associate> getAssociates() {
		return associates;
	}

	public void setAssociates(List<Associate> associates) {
		this.associates = associates;
	}
}
