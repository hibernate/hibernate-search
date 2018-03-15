/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorPath;

public class PojoAugmentedPropertyModel {

	public static final PojoAugmentedPropertyModel EMPTY = new PojoAugmentedPropertyModel(
			Collections.emptyMap(), Collections.emptyMap()
	);

	private final Map<ContainerValueExtractorPath, PojoAugmentedValueModel> values;
	private final Map<Class<?>, List<?>> markers;

	public PojoAugmentedPropertyModel(Map<ContainerValueExtractorPath, PojoAugmentedValueModel> values,
			Map<Class<?>, List<?>> markers) {
		this.values = values;
		this.markers = markers;
	}

	public PojoAugmentedValueModel getValue(ContainerValueExtractorPath extractorPath) {
		return values.getOrDefault( extractorPath, PojoAugmentedValueModel.EMPTY );
	}

	public Map<ContainerValueExtractorPath, PojoAugmentedValueModel> getAugmentedValues() {
		return values;
	}

	@SuppressWarnings("unchecked")
	public <M> Stream<M> getMarkers(Class<M> markerType) {
		return ( (List<M>) this.markers.getOrDefault( markerType, Collections.emptyList() ) )
				.stream();
	}


}
