/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;


import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeTerminalContext;
import org.hibernate.search.engine.backend.types.IndexFieldType;

/**
 * An element of the index schema,
 * allowing the definition of child fields.
 */
public interface IndexSchemaElement {

	/**
	 * Add a field to this index schema element with the given type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param type The type of the new field.
	 * @param <F> The type of values held by the field.
	 * @return A context allowing to get the reference to that new field.
	 */
	<F> IndexSchemaFieldContext<?, IndexFieldReference<F>> field(
			String relativeFieldName, IndexFieldType<F> type);

	/**
	 * Add a field to this index schema element with the given almost-built type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param terminalContext The almost-built type of the new field.
	 * @param <F> The type of values held by the field.
	 * @return A context allowing to get the reference to that new field.
	 */
	default <F> IndexSchemaFieldContext<?, IndexFieldReference<F>> field(
			String relativeFieldName, IndexFieldTypeTerminalContext<F> terminalContext) {
		return field( relativeFieldName, terminalContext.toIndexFieldType() );
	}

	/**
	 * Add a field to this index schema element with the type to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param typeContributor A function that will use the DSL context passed in parameter to create a type,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @param <F> The type of accessors for the new field.
	 * @return A context allowing to get the reference to that new field.
	 */
	<F> IndexSchemaFieldContext<?, IndexFieldReference<F>> field(String relativeFieldName,
			Function<? super IndexFieldTypeFactoryContext, ? extends IndexFieldTypeTerminalContext<F>> typeContributor);

	/**
	 * Add an object field to this index schema element with the default storage type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @return A context allowing to get the reference to that new object field and to add new child fields to it.
	 */
	default IndexSchemaObjectField objectField(String relativeFieldName) {
		return objectField( relativeFieldName, ObjectFieldStorage.DEFAULT );
	}

	/**
	 * Add an object field to this index schema element with the given storage type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param storage The storage type.
	 * @return A context allowing to get the reference to that new object field and to add new child fields to it.
	 * @see ObjectFieldStorage
	 */
	IndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage);

}
