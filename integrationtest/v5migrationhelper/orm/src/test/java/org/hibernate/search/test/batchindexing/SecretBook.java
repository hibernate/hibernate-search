/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * To cover the unusual case in which a non-indexed entity
 * extends an indexed entity.
 * It shouldn't be possible to find SecretBooks by using a
 * fulltext query.
 *
 * @author Sanne Grinovero
 */
@Entity
@Indexed(enabled = false)
public class SecretBook extends Book {

	boolean allCopiesBurnt = true;

	public boolean isAllCopiesBurnt() {
		return allCopiesBurnt;
	}

	public void setAllCopiesBurnt(boolean allCopiesBurnt) {
		this.allCopiesBurnt = allCopiesBurnt;
	}

}
