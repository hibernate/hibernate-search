/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import java.util.Set;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

@Embeddable
public class Embedded {

	@Field
	public String e1;

	@jakarta.persistence.ElementCollection
	public Set<Integer> e2;

	@IndexedEmbedded
	@OneToMany
	public Set<B> e3;

}
