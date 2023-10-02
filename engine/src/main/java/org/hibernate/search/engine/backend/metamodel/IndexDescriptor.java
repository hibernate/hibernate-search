/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.util.common.annotation.Incubating;

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

	/**
	 * Looks up the configured analyzers available to the index represented by this descriptor.
	 *
	 * @param name The name of the analyzer.
	 * @return An {@link Optional#empty() empty optional} if there is no analyzer configured with the given name.
	 */
	@Incubating
	Optional<? extends AnalyzerDescriptor> analyzer(String name);

	/**
	 * @return A collection of configured analyzer descriptors available to the index represented by this descriptor.
	 */
	@Incubating
	Collection<? extends AnalyzerDescriptor> analyzers();

	/**
	 * Looks up the configured normalizers available to the index represented by this descriptor.
	 *
	 * @param name The name of the normalizer.
	 * @return An {@link Optional#empty() empty optional} if there is no normalizer configured with the given name.
	 */
	@Incubating
	Optional<? extends NormalizerDescriptor> normalizer(String name);

	/**
	 * @return A collection of configured normalizer descriptors available to the index represented by this descriptor.
	 */
	@Incubating
	Collection<? extends NormalizerDescriptor> normalizers();

}
