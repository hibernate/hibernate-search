/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;

public interface IndexSchemaObjectNodeBuilder extends IndexSchemaContext {

	/**
	 * Create a new field and add it to the current builder.
	 *
	 * @param relativeFieldName The relative name of the new field
	 * @return A context allowing to define the new field
	 */
	IndexSchemaFieldContext addField(String relativeFieldName);

	/**
	 * Create a new field, but do not add it to the current builder.
	 * <p>
	 * This means in particular the field will not be added to the schema,
	 * and its accessor will not have any effect on documents.
	 *
	 * @param relativeFieldName The relative name of the new field
	 * @return A context allowing to define the new field
	 */
	IndexSchemaFieldContext createExcludedField(String relativeFieldName);

	/**
	 * Create a new object field and add it to the current builder.
	 *
	 * @param relativeFieldName The relative name of the new object field
	 * @param storage The storage type of the new object field
	 * @return A builder for the new object field
	 */
	IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage);

	/**
	 * Create a new object field, but do not add it to the current builder.
	 * <p>
	 * This means in particular the field will not be added to the schema,
	 * and its accessor will not have any effect on documents.
	 *
	 * @param relativeFieldName The relative name of the new object field
	 * @param storage The storage type of the new object field
	 * @return A builder for the new object field
	 */
	IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName, ObjectFieldStorage storage);

}
