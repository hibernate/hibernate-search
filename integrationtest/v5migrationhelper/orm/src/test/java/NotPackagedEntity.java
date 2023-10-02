/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * This entity is intentionally in no package for testing purposes
 */
@Indexed
@Entity
public class NotPackagedEntity {

	@GeneratedValue
	@Id
	long id;

	@Field
	String title;

}
