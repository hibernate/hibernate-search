/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.facet;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.IntegerBridge;

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

	@Field(analyze = Analyze.NO, bridge = @FieldBridge(impl = IntegerBridge.class))
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


