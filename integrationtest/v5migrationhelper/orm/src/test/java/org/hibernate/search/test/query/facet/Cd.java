/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.facet;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
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

	@Field
	private String name;

	@Field(analyze = Analyze.NO)
	@Facet
	private int price;

	@Field(analyze = Analyze.NO)
	@DateBridge(resolution = Resolution.YEAR)
	@Facet
	private Date releaseYear;

	public Cd() {
	}

	public Cd(String name, int price, String releaseYear) {
		this.name = name;
		this.price = price;
		DateFormat formatter = new SimpleDateFormat( "yyyy", Locale.ROOT );
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
