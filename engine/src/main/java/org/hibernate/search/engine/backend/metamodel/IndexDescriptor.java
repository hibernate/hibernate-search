/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;

/**
 * A descriptor of an index, exposing in particular the available fields and their characteristics.
 */
public interface IndexDescriptor {

	/**
	 * @return The name that uniquely identifies this index within the backend.
	 * This is the Hibernate Search name; the name of the index on the filesystem
	 * or in Elasticsearch may be different (lowercased, with a suffix, ...).
	 * See the reference documentation of your backend for more information.
	 */
	String hibernateSearchName();

	/**
	 * @return A descriptor of the {@link IndexCompositeElementDescriptor#isRoot() root element} of this index.
	 */
	IndexCompositeElementDescriptor root();

	/**
	 * Get a field by its path.
	 * <p>
	 * This method can find static fields as well as dynamic fields,
	 * unlike {@link #staticFields()}.
	 *
	 * @param absolutePath An absolute, dot-separated path.
	 * @return The corresponding field, or {@link Optional#empty()} if no field exists with this path.
	 */
	Optional<IndexFieldDescriptor> field(String absolutePath);

	/**
	 * Get all statically-defined fields for this index.
	 * <p>
	 * Only statically-defined fields are returned;
	 * fields created dynamically through {@link IndexSchemaElement#fieldTemplate(String, Function) templates}
	 * are not included in the collection.
	 *
	 * @return A collection containing all fields.
	 */
	Collection<IndexFieldDescriptor> staticFields();

}
