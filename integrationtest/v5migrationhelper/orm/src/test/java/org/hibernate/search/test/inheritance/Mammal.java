/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.inheritance;

import java.io.Serializable;

import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Mammal extends Animal implements Serializable {
	private boolean hasSweatGlands;

	@Field(analyze = Analyze.NO, store = Store.YES)
	public boolean isHasSweatGlands() {
		return hasSweatGlands;
	}

	public void setHasSweatGlands(boolean hasSweatGlands) {
		this.hasSweatGlands = hasSweatGlands;
	}
}
