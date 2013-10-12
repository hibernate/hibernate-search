/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.query.facet;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Car {
	@Id
	@GeneratedValue
	private int id;

	@Field(analyze = Analyze.NO)
	private String color;

	@Field(store = Store.YES)
	private String make;

	@Field(analyze = Analyze.NO)
	private int cubicCapacity;

	public Car() {
	}

	public Car(String make, String color, int cubicCapacity) {
		this.color = color;
		this.cubicCapacity = cubicCapacity;
		this.make = make;
	}

	public String getColor() {
		return color;
	}

	public int getCubicCapacity() {
		return cubicCapacity;
	}

	public int getId() {
		return id;
	}

	public String getMake() {
		return make;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Car" );
		sb.append( "{id=" ).append( id );
		sb.append( ", color='" ).append( color ).append( '\'' );
		sb.append( ", make='" ).append( make ).append( '\'' );
		sb.append( ", cubicCapacity=" ).append( cubicCapacity );
		sb.append( '}' );
		return sb.toString();
	}
}


