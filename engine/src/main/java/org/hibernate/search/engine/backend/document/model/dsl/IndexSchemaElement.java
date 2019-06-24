/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;


import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
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
	 * @return A DSL step where the field can be defined in more details,
	 * and where a {@link IndexSchemaObjectField#toReference() a reference to the field} can be obtained.
	 */
	<F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> field(
			String relativeFieldName, IndexFieldType<F> type);

	/**
	 * Add a field to this index schema element with the given almost-built type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param dslFinalStep A final step in the index field type DSL allowing the retrieval of an {@link IndexFieldType}.
	 * @param <F> The type of values held by the field.
	 * @return A DSL step where the field can be defined in more details,
	 * and where a {@link IndexSchemaObjectField#toReference() a reference to the field} can be obtained.
	 */
	default <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> field(
			String relativeFieldName, IndexFieldTypeFinalStep<F> dslFinalStep) {
		return field( relativeFieldName, dslFinalStep.toIndexFieldType() );
	}

	/**
	 * Add a field to this index schema element with the type to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param typeContributor A function that will use the factory passed in parameter to create a type,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @param <F> The type of accessors for the new field.
	 * @return A DSL step where the field can be defined in more details,
	 * and where a {@link IndexSchemaObjectField#toReference() a reference to the field} can be obtained.
	 */
	<F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> field(String relativeFieldName,
			Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<F>> typeContributor);

	/**
	 * Add an object field to this index schema element with the default storage type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @return An {@link IndexSchemaObjectField}, where the field can be defined in more details,
	 * in particular by adding new child fields to it,
	 * and where ultimately a {@link IndexSchemaObjectField#toReference() a reference to the field} can be obtained.
	 */
	default IndexSchemaObjectField objectField(String relativeFieldName) {
		return objectField( relativeFieldName, ObjectFieldStorage.DEFAULT );
	}

	/**
	 * Add an object field to this index schema element with the given storage type.
	 *
	 * @param relativeFieldName The relative name of the new field.
	 * @param storage The storage type.
	 * @return An {@link IndexSchemaObjectField}, where the field can be defined in more details,
	 * in particular by adding new child fields to it,
	 * and where ultimately a {@link IndexSchemaObjectField#toReference() a reference to the field} can be obtained.
	 * @see ObjectFieldStorage
	 */
	IndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage);

}
