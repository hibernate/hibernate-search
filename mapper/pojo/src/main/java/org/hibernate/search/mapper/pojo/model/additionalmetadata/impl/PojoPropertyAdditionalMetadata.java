/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

public class PojoPropertyAdditionalMetadata {

	public static final PojoPropertyAdditionalMetadata EMPTY = new PojoPropertyAdditionalMetadata(
			Collections.emptyMap(), Collections.emptyMap()
	);

	private final Map<ContainerExtractorPath, PojoValueAdditionalMetadata> valuesAdditionalMetadata;
	private final Map<Class<?>, List<?>> markers;

	public PojoPropertyAdditionalMetadata(Map<ContainerExtractorPath,
			PojoValueAdditionalMetadata> valuesAdditionalMetadata,
			Map<Class<?>, List<?>> markers) {
		this.valuesAdditionalMetadata = valuesAdditionalMetadata;
		this.markers = markers;
	}

	public PojoValueAdditionalMetadata getValueAdditionalMetadata(ContainerExtractorPath extractorPath) {
		return valuesAdditionalMetadata.getOrDefault( extractorPath, PojoValueAdditionalMetadata.EMPTY );
	}

	public Map<ContainerExtractorPath, PojoValueAdditionalMetadata> getValuesAdditionalMetadata() {
		return valuesAdditionalMetadata;
	}

	@SuppressWarnings("unchecked")
	public <M> Stream<M> getMarkers(Class<M> markerType) {
		return ( (List<M>) this.markers.getOrDefault( markerType, Collections.emptyList() ) )
				.stream();
	}
}
