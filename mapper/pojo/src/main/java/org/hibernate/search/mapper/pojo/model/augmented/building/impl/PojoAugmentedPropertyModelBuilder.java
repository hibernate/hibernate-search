/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.augmented.building.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedPropertyModel;

class PojoAugmentedPropertyModelBuilder implements PojoAugmentedModelCollectorPropertyNode {
	private final Map<Class<?>, List<?>> markers = new HashMap<>();

	@Override
	public final void marker(MarkerBuilder builder) {
		doAddMarker( builder.build() );
	}

	@SuppressWarnings("unchecked")
	private <M> void doAddMarker(M marker) {
		Class<M> markerType = (Class<M>) (
				marker instanceof Annotation ? ((Annotation) marker).annotationType()
						: marker.getClass()
		);
		List<M> list = (List<M>) markers.computeIfAbsent( markerType, ignored -> new ArrayList<M>() );
		list.add( marker );
	}

	PojoAugmentedPropertyModel build() {
		return new PojoAugmentedPropertyModel( markers );
	}

}
