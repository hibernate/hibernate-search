/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document;


/**
 * A reference to an "object" field of an indexed document,
 * allowing to add new values to this field for a given document.
 *
 * @see DocumentElement#addObject(IndexObjectFieldReference)
 * @see DocumentElement#addNullObject(IndexObjectFieldReference)
 */
public interface IndexObjectFieldReference {

	// FIXME Remove this method and usages of this method
	@Deprecated
	default DocumentElement add(DocumentElement target) {
		return target.addObject( this );
	}

	// FIXME Remove this method and usages of this method
	@Deprecated
	default void addMissing(DocumentElement target) {
		target.addNullObject( this );
	}
}
