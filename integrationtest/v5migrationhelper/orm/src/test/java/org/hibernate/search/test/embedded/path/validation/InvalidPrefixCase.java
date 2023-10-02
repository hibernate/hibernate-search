/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author zkurey
 *
 *         The "a." prefix shouldn't be specified since its implicit that "a." is
 *         part of the path
 */
@Entity
@Indexed
public class InvalidPrefixCase {

	@Id
	@GeneratedValue
	public int id;

	@ManyToOne
	@IndexedEmbedded(includePaths = {
			"b.c.indexed", // valid
			"a.b.c.indexed" // invalid, prefix of a. is not necessary, as path is relative to a
	})
	public A a;

}
