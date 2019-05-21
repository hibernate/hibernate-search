/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorDefinitionContext;

@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public class ContainerExtractorDefinitionContextImpl implements ContainerExtractorDefinitionContext {
	private final Map<String, Class<? extends ContainerExtractor>> extractorsByName = new HashMap<>();

	@Override
	public void define(String extractorName, Class<? extends ContainerExtractor> extractorClass) {
		extractorsByName.put( extractorName, extractorClass );
	}

	Map<String, Class<? extends ContainerExtractor>> getExtractorsByName() {
		return Collections.unmodifiableMap( extractorsByName );
	}
}
