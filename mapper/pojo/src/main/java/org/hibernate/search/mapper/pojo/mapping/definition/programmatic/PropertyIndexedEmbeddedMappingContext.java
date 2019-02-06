/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;

/**
 * @author Yoann Rodiere
 */
public interface PropertyIndexedEmbeddedMappingContext extends PropertyMappingContext {

	PropertyIndexedEmbeddedMappingContext prefix(String prefix);

	PropertyIndexedEmbeddedMappingContext storage(ObjectFieldStorage storage);

	PropertyIndexedEmbeddedMappingContext maxDepth(Integer depth);

	default PropertyIndexedEmbeddedMappingContext includePaths(String ... paths) {
		return includePaths( Arrays.asList( paths ) );
	}

	PropertyIndexedEmbeddedMappingContext includePaths(Collection<String> paths);

	@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
	default PropertyIndexedEmbeddedMappingContext withExtractor(
			Class<? extends ContainerExtractor> extractorClass) {
		return withExtractors( ContainerExtractorPath.explicitExtractor( extractorClass ) );
	}

	default PropertyIndexedEmbeddedMappingContext withoutExtractors() {
		return withExtractors( ContainerExtractorPath.noExtractors() );
	}

	PropertyIndexedEmbeddedMappingContext withExtractors(ContainerExtractorPath extractorPath);

}
