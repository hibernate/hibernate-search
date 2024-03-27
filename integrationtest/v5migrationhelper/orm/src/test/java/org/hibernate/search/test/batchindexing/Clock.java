/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Clock {
	private Integer id;
	private String brand;

	public Clock() {
	}

	public Clock(Integer id, String brand) {
		this.id = id;
		this.brand = brand;
	}

	@Field(store = Store.YES)
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	@Id
	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "[" );
		builder.append( id );
		builder.append( "," );
		builder.append( brand );
		builder.append( "]" );
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( brand == null ) ? 0 : brand.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		Clock other = (Clock) obj;
		if ( brand == null ) {
			if ( other.brand != null ) {
				return false;
			}
		}
		else if ( !brand.equals( other.brand ) ) {
			return false;
		}
		return true;
	}
}
