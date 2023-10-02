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
 */
@Entity
@Indexed
public class PathNotIndexedCase {

	@Id
	@GeneratedValue
	public int id;

	@ManyToOne
	@IndexedEmbedded(depth = 4, includePaths = { "c.indexed" })
	public ReferencesC e;

}
