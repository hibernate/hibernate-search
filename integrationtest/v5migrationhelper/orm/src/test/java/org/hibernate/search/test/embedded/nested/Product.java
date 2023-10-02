/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.nested;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Product {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@IndexedEmbedded
	private List<Attribute> attributes;

	public Product() {
		attributes = new ArrayList<Attribute>();
	}

	public long getId() {
		return id;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttribute(Attribute attribute) {
		attributes.add( attribute );
	}
}
