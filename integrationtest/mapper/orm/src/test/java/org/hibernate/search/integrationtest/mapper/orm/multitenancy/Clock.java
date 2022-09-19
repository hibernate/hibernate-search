/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.multitenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@PortedFromSearch5(original = "org.hibernate.search.test.batchindexing.Clock")
public class Clock {

	public static final String INDEX = "Clock";

	private Integer id;
	private String brand;

	public Clock() {
	}

	public Clock(Integer id, String brand) {
		this.id = id;
		this.brand = brand;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@GenericField
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
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
