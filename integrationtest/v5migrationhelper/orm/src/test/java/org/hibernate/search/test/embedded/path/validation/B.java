/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
public class B {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne(mappedBy = "b")
	public A a;

	@OneToOne(mappedBy = "b2")
	public A a2;

	@OneToOne
	@IndexedEmbedded
	public C c;

	@OneToOne
	@IndexedEmbedded
	public C skipped;

	public B() {
	}

	public B(C c, C skipped) {
		this.c = c;
		c.b = this;

		if ( skipped != null ) {
			this.skipped = skipped;
			skipped.b = this;
		}
	}

}
