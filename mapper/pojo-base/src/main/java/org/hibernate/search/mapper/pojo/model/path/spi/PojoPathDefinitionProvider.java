/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.List;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * A static definition of POJO paths for a given entity type, allowing the creation of path filters.
 */
public interface PojoPathDefinitionProvider {

	/**
	 * @return The string representations of pre-defined paths, with their index matching their intended ordinal.
	 */
	List<String> preDefinedOrdinals();

	/**
	 * @param source A set of paths to check for correctness (can it be ever marked as dirty?)
	 * and to turn into their string representation.
	 * The set must be non-null and non-empty, and the elements must be non-null.
	 * Container value extractor paths must be completely resolved:
	 * {@link ContainerExtractorPath#defaultExtractors()} is an invalid value
	 * that must never appear in the given paths.
	 * @return A definition of the given path.
	 */
	PojoPathDefinition interpretPath(PojoModelPathValueNode source);

}
