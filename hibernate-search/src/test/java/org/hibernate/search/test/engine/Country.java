package org.hibernate.search.test.engine;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import javax.persistence.Embeddable;

/**
 * author: Gustavo Fernandes
 */
@Embeddable
public class Country {

	@Field( store = Store.YES )
	@NumericField
	private double idh;

	@Field( store = Store.YES )
	private String name;

	public Country(String name, double idh) {
		this.name = name;
		this.idh = idh;
	}

	public Country() {
	}

}
