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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Resolution;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Cd {

	@Id
	@GeneratedValue
	private int id;

	@Fields({
			@Field,
			@Field(name = "name_un_analyzed", analyze = Analyze.NO)
	})
	private String name;

	@Field(analyze = Analyze.NO)
	@NumericField
	private int price;

	@Field(analyze = Analyze.NO)
	@DateBridge(resolution = Resolution.YEAR)
	private Date releaseYear;

	public Cd() {
	}

	public Cd(String name, int price, String releaseYear) {
		this.name = name;
		this.price = price;
		DateFormat formatter = new SimpleDateFormat( "yyyy" );
		try {
			this.releaseYear = formatter.parse( releaseYear );
		}
		catch (ParseException e) {
			throw new IllegalArgumentException( "wrong date format" );
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getPrice() {
		return price;
	}

	public Date getReleaseYear() {
		return releaseYear;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Cd" );
		sb.append( "{id=" ).append( id );
		sb.append( ", name='" ).append( name ).append( '\'' );
		sb.append( ", price=" ).append( price );
		sb.append( ", releaseYear=" ).append( releaseYear );
		sb.append( '}' );
		return sb.toString();
	}
}
