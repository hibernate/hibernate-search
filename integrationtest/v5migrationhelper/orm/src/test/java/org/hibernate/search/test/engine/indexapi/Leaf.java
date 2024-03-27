/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.engine.indexapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Leaf {

	@Id
	@GeneratedValue
	private int id;

	@ManyToOne
	private Tree tree;

	public int getId() {
		return id;
	}

	public Tree getTree() {
		return tree;
	}
}

