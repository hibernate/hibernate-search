/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document;

import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldFinalStep;
import org.hibernate.search.engine.backend.types.ObjectStructure;

/**
 * An element of a document.
 * <p>
 * Instances may represent the document root as well as a <em>partial</em> view of the document,
 * for instance a view on a specific "object" field nested inside the document.
 *
 */
public interface DocumentElement {

	/**
	 * Add a new value to a field in this document element.
	 * <p>
	 * This method can be called multiple times for the same field,
	 * which will result in multiple values being added to the same field.
	 *
	 * @param fieldReference A reference to the field to add a value to.
	 * References are returned by {@link IndexSchemaFieldFinalStep#toReference()}
	 * in the {@link IndexSchemaElement#field(String, Function) field definition DSL}.
	 * @param value The value to add to the field.
	 * @param <F> The type of values for the given field.
	 * @throws org.hibernate.search.util.common.SearchException If the field is defined in a different document element.
	 */
	<F> void addValue(IndexFieldReference<F> fieldReference, F value);

	/**
	 * Add a new object to a field in this document element.
	 *
	 * @param fieldReference A reference to the object field to add an object to.
	 * References are returned by {@link IndexSchemaFieldFinalStep#toReference()}
	 * in the {@link IndexSchemaElement#objectField(String, ObjectStructure) field definition DSL}.
	 * @return The new object, that can be populated with its own fields.
	 * @throws org.hibernate.search.util.common.SearchException If the field is defined in a different document element.
	 */
	DocumentElement addObject(IndexObjectFieldReference fieldReference);

	/**
	 * Add a {@code null} object to the referenced field in this document element.
	 * <p>
	 * The {@code null} object may have a representation in the backend (such as a JSON {@code null}),
	 * or it may be ignored completely, depending on the backend implementation.
	 *
	 * @param fieldReference A reference to the object field to add a {@code null} object to.
	 * References are returned by {@link IndexSchemaFieldFinalStep#toReference()}
	 * in the {@link IndexSchemaElement#objectField(String, ObjectStructure) field definition DSL}.
	 * @throws org.hibernate.search.util.common.SearchException If the field is defined in a different document element.
	 */
	void addNullObject(IndexObjectFieldReference fieldReference);

	/**
	 * Add a new value to a field in this document element.
	 * <p>
	 * This method can be called multiple times for the same field,
	 * which will result in multiple values being added to the same field.
	 *
	 * @param relativeFieldName The name of the field to add a value to, relative to this document element.
	 * The field must have been defined previously, either directly
	 * through {@link IndexSchemaElement#field(String, Function)},
	 * or indirectly through {@link IndexSchemaElement#fieldTemplate(String, Function)}.
	 * @param value The value to add to the field.
	 * @throws org.hibernate.search.util.common.SearchException If the field is not defined
	 * or if {@code value} has an incorrect type of this field.
	 */
	void addValue(String relativeFieldName, Object value);

	/**
	 * Add a new object to a field in this document element.
	 *
	 * @param relativeFieldName The name of the object field to add a value to, relative to this document element.
	 * The field must have been defined previously, either directly
	 * through {@link IndexSchemaElement#objectField(String, ObjectStructure)},
	 * or indirectly through {@link IndexSchemaElement#objectFieldTemplate(String, ObjectStructure)}.
	 * @return The new object, that can be populated with its own fields.
	 * @throws org.hibernate.search.util.common.SearchException If the field is not defined
	 * or is not an object field.
	 */
	DocumentElement addObject(String relativeFieldName);

	/**
	 * Add a {@code null} object to a field in this document element.
	 * <p>
	 * The {@code null} object may have a representation in the backend (such as a JSON {@code null}),
	 * or it may be ignored completely, depending on the backend implementation.
	 *
	 * @param relativeFieldName The name of the object field to add a value to, relative to this document element.
	 * The field must have been defined previously, either directly
	 * through {@link IndexSchemaElement#objectField(String, ObjectStructure)},
	 * or indirectly through {@link IndexSchemaElement#objectFieldTemplate(String, ObjectStructure)}.
	 * @throws org.hibernate.search.util.common.SearchException If the field is not defined
	 * or is not an object field.
	 */
	void addNullObject(String relativeFieldName);

}
