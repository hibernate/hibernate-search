/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor;

import java.util.List;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

/**
 * A context to assign names to container extractor implementations.
 *
 * @see ContainerExtractor
 * @see ContainerExtractorPath#explicitExtractor(String)
 * @see ContainerExtractorPath#explicitExtractors(List)
 */
@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public interface ContainerExtractorConfigurationContext {

	void define(String extractorName, Class<? extends ContainerExtractor> extractorClass);

}
