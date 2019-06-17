/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document;

/**
 * An element of a document.
 * <p>
 * Instances may represent the document root as well as a <em>partial</em> view of the document,
 * for instance a view on a specific "object" field nested inside the document.
 *
 */
public interface DocumentElement {

	/**
	 * Add a new value to the referenced field in this document element.
	 * <p>
	 * This method can be called multiple times for the same field,
	 * which will result in multiple values being added to the same field.
	 *
	 * @param fieldReference The field to add a value to.
	 * @param value The value to add to the field.
	 * @param <F> The type of values for the given field.
	 */
	<F> void addValue(IndexFieldReference<F> fieldReference, F value);

	/**
	 * Add a new object to the referenced field in this document element.
	 *
	 * @param fieldReference The object field to add an object to.
	 * @return The new object, that can be populated with its own fields.
	 */
	DocumentElement addObject(IndexObjectFieldReference fieldReference);

	/**
	 * Add a {@code null} object to the referenced field in this document element.
	 * <p>
	 * The {@code null} object may have a representation in the backend (such as a JSON {@code null}),
	 * or it may be ignored completely, depending on the backend implementation.
	 *
	 * @param fieldReference The object field to add a {@code null} object to.
	 */
	void addNullObject(IndexObjectFieldReference fieldReference);

}
