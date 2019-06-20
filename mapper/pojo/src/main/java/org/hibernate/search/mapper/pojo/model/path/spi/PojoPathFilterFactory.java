/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * A factory for {@link PojoPathFilter} instances.
 *
 * @param <S> The expected type of the object representing a set of paths.
 */
public interface PojoPathFilterFactory<S> {

	/**
	 * @param paths The set of paths to test for dirtiness.
	 * The set must be non-null and non-empty, and the elements must be non-null.
	 * Container value extractor paths must be completely resolved:
	 * {@link ContainerExtractorPath#defaultExtractors()} is an invalid value
	 * that must never appear in the given paths.
	 * @return A filter accepting only the given paths.
	 */
	PojoPathFilter<S> create(Set<PojoModelPathValueNode> paths);

}
