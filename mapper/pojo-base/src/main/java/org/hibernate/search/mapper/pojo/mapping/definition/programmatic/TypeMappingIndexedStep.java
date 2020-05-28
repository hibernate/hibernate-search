/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * The step in a mapping definition where a type's indexing can be configured more precisely.
 */
public interface TypeMappingIndexedStep {

	/**
	 * @param backendName The name of the backend.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#backend()
	 */
	TypeMappingIndexedStep backend(String backendName);

	/**
	 * @param indexName The name of the index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#index()
	 */
	TypeMappingIndexedStep index(String indexName);

	/**
	 * @param enabled {@code true} to map the type to an index (the default),
	 * {@code false} to disable the mapping to an index.
	 * Useful to disable indexing when subclassing an indexed type.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#enabled()
	 */
	TypeMappingIndexedStep enabled(boolean enabled);

}
