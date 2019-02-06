/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * @author Yoann Rodiere
 */
public interface IndexingDependencyMappingContext extends PropertyMappingContext {

	IndexingDependencyMappingContext reindexOnUpdate(ReindexOnUpdate reindexOnUpdate);

	IndexingDependencyMappingContext derivedFrom(PojoModelPathValueNode pojoModelPath);

	@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
	default IndexingDependencyMappingContext withExtractor(
			Class<? extends ContainerExtractor> extractorClass) {
		return withExtractors( ContainerExtractorPath.explicitExtractor( extractorClass ) );
	}

	default IndexingDependencyMappingContext withoutExtractors() {
		return withExtractors( ContainerExtractorPath.noExtractors() );
	}

	IndexingDependencyMappingContext withExtractors(ContainerExtractorPath extractorPath);

}
