/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

/**
 * Determines how to encode the indexed value.
 *
 * @author Hardy Ferentschik
 */
public enum FacetEncodingType {
	/**
	 * Facet values are stored as strings using {@code SortedSetDocValuesFacetField}
	 */
	STRING,

	/**
	 * Facet values are stored as long values using {@code NumericDocValuesField}
	 */
	LONG,

	/**
	 * Facet values are stored as double values using {@code DoubleDocValuesField}
	 */
	DOUBLE,

	/**
	 * The encoding type for the facet is determined by the type of the entity property
	 */
	AUTO
}
