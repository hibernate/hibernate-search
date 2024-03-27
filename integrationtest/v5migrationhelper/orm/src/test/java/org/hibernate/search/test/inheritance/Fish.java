/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.inheritance;

import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Fish extends Animal {

	private int numberOfDorsalFins;

	@Field(analyze = Analyze.NO, store = Store.YES)
	public int getNumberOfDorsalFins() {
		return numberOfDorsalFins;
	}

	public void setNumberOfDorsalFins(int numberOfDorsalFins) {
		this.numberOfDorsalFins = numberOfDorsalFins;
	}
}
