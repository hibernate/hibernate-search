/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@Indexed
@Entity
public class CommunityCollege extends College {

	CommunityCollege() {
	}

	public CommunityCollege(long identifier, String name) {
		super( identifier, name );
	}
}
