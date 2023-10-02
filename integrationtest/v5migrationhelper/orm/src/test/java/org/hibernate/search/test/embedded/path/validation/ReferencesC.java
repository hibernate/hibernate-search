/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Indexed;

/**
 * @author zkurey
 */
@Entity
@Indexed
public class ReferencesC {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne
	//intentionally not @IndexedEmbedded
	public C c;

}
