/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.graph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
public class Event implements Serializable {

	private Long id;
	private Set<ParentOfBirthEvent> parentsOf = new HashSet<ParentOfBirthEvent>();
	private Set<Person> children = new HashSet<Person>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@IndexedEmbedded
	@OneToMany(mappedBy = "event")
	public Set<ParentOfBirthEvent> getParentsOf() {
		return parentsOf;
	}

	public void setParentsOf(Set<ParentOfBirthEvent> parentsOf) {
		this.parentsOf = parentsOf;
	}

	@OneToMany(mappedBy = "birthEvent")
	public Set<Person> getChildren() {
		return children;
	}

	public void setChildren(Set<Person> children) {
		this.children = children;
	}

}
