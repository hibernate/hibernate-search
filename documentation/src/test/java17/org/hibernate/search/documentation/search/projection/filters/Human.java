/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection.filters;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

// tag::include[]
@Entity
@Indexed
public class Human {

	@Id
	private Integer id;

	@FullTextField(analyzer = "name", projectable = Projectable.YES)
	private String name;

	@FullTextField(analyzer = "name", projectable = Projectable.YES)
	private String nickname;

	@ManyToMany
	@IndexedEmbedded(includeDepth = 5, structure = ObjectStructure.NESTED)
	private List<Human> parents = new ArrayList<>();

	@ManyToMany(mappedBy = "parents")
	private List<Human> children = new ArrayList<>();

	public Human() {
	}

	// Getters and setters
	// ...

	// tag::getters-setters[]
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

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public List<Human> getParents() {
		return parents;
	}

	public void setParents(List<Human> parents) {
		this.parents = parents;
	}

	public List<Human> getChildren() {
		return children;
	}

	public void setChildren(List<Human> children) {
		this.children = children;
	}
	// end::getters-setters[]
}
// end::include[]
