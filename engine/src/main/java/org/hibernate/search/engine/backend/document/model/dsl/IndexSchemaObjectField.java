/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

/**
 * An object field in the index schema,
 * allowing the definition of child fields,
 * customization of some characteristics of this object field such as {@link #multiValued() multi-valued-ness},
 * and the retrieval of {@link #toReference() a field reference} to be used when indexing.
 *
 * @see IndexSchemaElement
 * @see IndexSchemaFieldContext
 * @see IndexSchemaFieldTerminalContext
 */
public interface IndexSchemaObjectField
		extends IndexSchemaElement, IndexSchemaFieldContext<IndexSchemaObjectField, IndexObjectFieldReference> {

}
