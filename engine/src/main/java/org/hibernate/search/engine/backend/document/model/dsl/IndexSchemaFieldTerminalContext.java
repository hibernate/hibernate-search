/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

/**
 * The terminal context when specifying an index field.
 *
 * @param <R> The reference type.
 */
public interface IndexSchemaFieldTerminalContext<R> {

	/**
	 * Create a reference to this field
	 * for use when indexing.
	 *
	 * @return The reference to use when indexing.
	 * @see DocumentElement#addValue(IndexFieldReference, Object)
	 * @see DocumentElement#addObject(IndexObjectFieldReference)
	 * @see DocumentElement#addNullObject(IndexObjectFieldReference)
	 */
	R toReference();

}
