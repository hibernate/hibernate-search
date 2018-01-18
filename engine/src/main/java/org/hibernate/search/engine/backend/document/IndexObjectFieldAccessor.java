/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document;


/**
 * An accessor to an "object" field of a index document,
 * allowing to add new values to this field for a given document.
 *
 * @author Yoann Rodiere
 */
public interface IndexObjectFieldAccessor {

	/**
	 * Add a new object to the given state at the target of this reference,
	 * and return the nested object, so that it can be populated with fields.
	 *
	 * @param target The parent object to which the nested object will be added.
	 * @return The nested object.
	 */
	DocumentElement add(DocumentElement target);

	/**
	 * Similar to {@link #add(DocumentElement)}, but instead of adding an
	 * object, this method adds a missing value marker.
	 * <p>
	 * The missing value marker may have a representation in the backend (such as a JSON {@code null}),
	 * or it may be ignored completely, depending on the implementation.
	 *
	 * @param target The parent object to which the "missing value marker" will be added.
	 */
	void addMissing(DocumentElement target);

}
