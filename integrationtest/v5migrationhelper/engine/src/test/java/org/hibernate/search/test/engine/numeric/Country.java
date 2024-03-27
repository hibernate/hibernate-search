/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.numeric;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

/**
 * author: Gustavo Fernandes
 */
class Country {

	@Field(store = Store.YES)
	private double idh;

	@Field(store = Store.YES)
	private String name;

	public Country(String name, double idh) {
		this.name = name;
		this.idh = idh;
	}

	public Country() {
	}

}
