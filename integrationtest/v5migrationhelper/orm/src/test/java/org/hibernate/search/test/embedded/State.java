/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class State {
	@Id
	@DocumentId
	@GeneratedValue
	private Integer id;

	@Field
	private String name;

	@OneToOne(mappedBy = "state", cascade = CascadeType.ALL)
	private StateCandidate candidate;

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

	public StateCandidate getCandidate() {
		return candidate;
	}

	public void setCandidate(StateCandidate candidate) {
		this.candidate = candidate;
	}
}
