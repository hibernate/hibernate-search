/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * A context to define how a dependency of the indexing process to the property
 * should affect automatic indexing.
 */
public interface IndexingDependencyMappingContext extends PropertyMappingContext {

	/**
	 * @param reindexOnUpdate How indexed entities using the annotated property should be reindexed when the value,
	 * or any nested value, is updated.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#reindexOnUpdate()
	 * @see ReindexOnUpdate
	 */
	IndexingDependencyMappingContext reindexOnUpdate(ReindexOnUpdate reindexOnUpdate);

	/**
	 * @param pojoModelPath A path to other values that are used to generate the value of this property.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#derivedFrom()
	 * @see PojoModelPathValueNode
	 * @see PojoModelPath#ofValue(String)
	 * @see PojoModelPath#ofValue(String, ContainerExtractorPath)
	 * @see PojoModelPath#builder()
	 */
	IndexingDependencyMappingContext derivedFrom(PojoModelPathValueNode pojoModelPath);

	/**
	 * @param extractorName The name of the container extractor to use.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#extraction()
	 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
	 */
	default IndexingDependencyMappingContext withExtractor(String extractorName) {
		return withExtractors( ContainerExtractorPath.explicitExtractor( extractorName ) );
	}

	/**
	 * Indicates that no container extractors should be applied,
	 * not even the default ones.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#extraction()
	 */
	default IndexingDependencyMappingContext withoutExtractors() {
		return withExtractors( ContainerExtractorPath.noExtractors() );
	}

	/**
	 * @param extractorPath A {@link ContainerExtractorPath}.
	 * @return {@code this}, for method chaining.
	 * @see IndexingDependency#extraction()
	 * @see ContainerExtractorPath
	 */
	IndexingDependencyMappingContext withExtractors(ContainerExtractorPath extractorPath);

}
