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
 * @param <F> The indexed field value type.
 *
 * @see DocumentElement#addValue(IndexFieldReference, Object)
 */
@SuppressWarnings("unused") // The type parameter is used in methods accepting this interface as a parameter.
public interface IndexFieldReference<F> {

}
