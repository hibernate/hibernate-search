/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed
public class Truck {
	@Id
	@GeneratedValue
	private int id;

	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES, indexNullAs = "-1")
	@Facet
	private Integer horsePower;

	public Truck() {
	}

	public Truck(Integer horsePower) {
		this.horsePower = horsePower;
	}

	public int getId() {
		return id;
	}

	public Integer getHorsePower() {
		return horsePower;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Truck" );
		sb.append( "{id=" ).append( id );
		sb.append( ", horsePower='" ).append( horsePower );
		sb.append( '}' );
		return sb.toString();
	}
}

