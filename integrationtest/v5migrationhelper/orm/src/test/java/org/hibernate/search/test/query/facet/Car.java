/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.facet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.testsupport.AnalysisNames;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Car {

	public static final String COLLATING_NORMALIZER_NAME = AnalysisNames.NORMALIZER_LOWERCASE_ASCIIFOLDING;

	public static final String CUBIC_CAPACITY_STRING = "cubicCapacity_string";

	// Those facet names must be different from the source field name, for testing purposes
	public static final String CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING = "cubicCapacity_string_facet_string";
	public static final String CUBIC_CAPACITY_STRING_FACET_NUMERIC_ENCODING = "cubicCapacity_string_facet_numeric";

	@Id
	@GeneratedValue
	private int id;

	@Field(analyze = Analyze.NO)
	@Facet
	private String color;

	@Field(analyze = Analyze.NO, store = Store.YES)
	@Facet
	private String make;

	@Field
	@Facet(name = CUBIC_CAPACITY_STRING_FACET_NUMERIC_ENCODING)
	private Integer cubicCapacity;

	public Car() {
	}

	public Car(String make, String color, Integer cubicCapacity) {
		this.color = color;
		this.cubicCapacity = cubicCapacity;
		this.make = make;
	}

	public String getColor() {
		return color;
	}

	public Integer getCubicCapacity() {
		return cubicCapacity;
	}

	@Field(name = CUBIC_CAPACITY_STRING, analyze = Analyze.NO)
	@Facet(name = CUBIC_CAPACITY_STRING_FACET_STRING_ENCODING, forField = CUBIC_CAPACITY_STRING)
	@IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "cubicCapacity")))
	public String getCubicCapacityString() {
		return cubicCapacity == null ? null : cubicCapacity.toString();
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


