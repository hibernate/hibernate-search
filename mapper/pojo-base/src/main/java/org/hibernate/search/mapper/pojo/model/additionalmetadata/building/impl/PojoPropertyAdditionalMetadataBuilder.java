/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.MarkerBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;

class PojoPropertyAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorPropertyNode {
	private final BeanResolver beanResolver;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<ContainerExtractorPath, PojoValueAdditionalMetadataBuilder> valueBuilders =
			new LinkedHashMap<>();
	private final Map<Class<?>, List<?>> markers = new LinkedHashMap<>();

	PojoPropertyAdditionalMetadataBuilder(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	@Override
	public PojoAdditionalMetadataCollectorValueNode value(ContainerExtractorPath extractorPath) {
		return valueBuilders.computeIfAbsent(
				extractorPath,
				path -> new PojoValueAdditionalMetadataBuilder()
		);
	}

	@Override
	public final void markerBinder(MarkerBinder binder, Map<String, Object> params) {
		MarkerBindingContextImpl bindingContext = new MarkerBindingContextImpl( beanResolver, params );
		doAddMarker( bindingContext.applyBinder( binder ) );
	}

	@SuppressWarnings("unchecked")
	private <M> void doAddMarker(M marker) {
		Class<M> markerType = (Class<M>) ( marker instanceof Annotation
				? ( (Annotation) marker ).annotationType()
				: marker.getClass() );
		List<M> list = (List<M>) markers.computeIfAbsent( markerType, ignored -> new ArrayList<M>() );
		list.add( marker );
	}

	PojoPropertyAdditionalMetadata build() {
		Map<ContainerExtractorPath, PojoValueAdditionalMetadata> values = new HashMap<>();
		for ( Map.Entry<ContainerExtractorPath, PojoValueAdditionalMetadataBuilder> entry : valueBuilders.entrySet() ) {
			values.put( entry.getKey(), entry.getValue().build() );
		}
		markers.replaceAll( (key, list) -> Collections.unmodifiableList( list ) );
		return new PojoPropertyAdditionalMetadata( values, markers );
	}

}
