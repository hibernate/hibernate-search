/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelType;

/**
 * A bridge between a POJO type and an element of the index schema.
 * <p>
 * The {@code TypeBridge} interface is a more powerful version of {@link ValueBridge}
 * that applies to a whole type instead of a single property,
 * and can contribute more than one index field, in particular.
 *
 * @author Yoann Rodiere
 */
public interface TypeBridge extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given index schema element and the given POJO model element.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Declare its expectations regarding the POJO model (input type, expected properties and their type, ...).
	 *     <li>Declare its expectations regarding the index schema (field names and field types, storage options, ...).
	 *     <li>Retrieve accessors to the POJO and to the index fields that will later be used in the
	 *     {@link #write(DocumentElement, PojoElement)} method.
	 *     <li>Optionally, using the {@link SearchModel}, define how to map POJO properties to fields when searching
	 *     (in predicates and projections).
	 * </ul>
	 *
	 * @param indexSchemaElement An entry point to declaring expectations and retrieving accessors to the index schema.
	 * @param bridgedPojoModelType An entry point to declaring expectations and retrieving accessors to the
	 * bridged POJO type.
	 * @param searchModel An entry point to defining how to map POJO properties to fields when searching.
	 */
	void bind(IndexSchemaElement indexSchemaElement, PojoModelType bridgedPojoModelType,
			SearchModel searchModel);

	/**
	 * Write to fields in the given {@link DocumentElement},
	 * using the given {@link PojoElement} as input and transforming it as necessary.
	 * <p>
	 * Writing to the {@link DocumentElement} should be done using
	 * {@link org.hibernate.search.engine.backend.document.IndexFieldAccessor}s retrieved when the
	 * {@link #bind(IndexSchemaElement, PojoModelType, SearchModel)} method was called.
	 * <p>
	 * Reading from the {@link PojoElement} should be done using
	 * {@link org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor}s retrieved when the
	 * {@link #bind(IndexSchemaElement, PojoModelType, SearchModel)} method was called.
	 *
	 * @param target The {@link DocumentElement} to write to.
	 * @param source The {@link PojoElement} to read from.
	 */
	void write(DocumentElement target, PojoElement source);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
