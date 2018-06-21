/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * @author Yoann Rodiere
 */
public interface IndexingDependencyMappingContext extends PropertyMappingContext {

	IndexingDependencyMappingContext reindexOnUpdate(ReindexOnUpdate reindexOnUpdate);

	IndexingDependencyMappingContext derivedFrom(PojoModelPathValueNode pojoModelPath);

	default IndexingDependencyMappingContext withExtractor(
			Class<? extends ContainerValueExtractor> extractorClass) {
		return withExtractors( ContainerValueExtractorPath.explicitExtractor( extractorClass ) );
	}

	default IndexingDependencyMappingContext withoutExtractors() {
		return withExtractors( ContainerValueExtractorPath.noExtractors() );
	}

	IndexingDependencyMappingContext withExtractors(ContainerValueExtractorPath extractorPath);

}
