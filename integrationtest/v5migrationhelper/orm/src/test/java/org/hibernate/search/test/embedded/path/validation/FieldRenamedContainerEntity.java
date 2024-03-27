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
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
class FieldRenamedContainerEntity {

	@Id
	@GeneratedValue
	Integer id;

	@OneToOne
	@IndexedEmbedded(depth = 0, includePaths = { "embedded.field" })
	FieldRenamedEmbeddedEntity embedded;
}
