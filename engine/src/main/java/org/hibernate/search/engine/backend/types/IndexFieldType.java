/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types;

/**
 * The type of a field in the index.
 * <p>
 * Used when defining the index schema.
 *
 * @param <F> The Java type of values held by fields.
 *
 * @see org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement#field(String, IndexFieldType)
 */
public interface IndexFieldType<F> {
}
