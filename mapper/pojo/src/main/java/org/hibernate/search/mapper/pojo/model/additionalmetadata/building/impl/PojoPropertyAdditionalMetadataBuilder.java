/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.logging.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.engine.logging.spi.ContextualFailureCollector;

class PojoPropertyAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorPropertyNode {
	private final PojoTypeAdditionalMetadataBuilder rootBuilder;
	private final String propertyName;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<ContainerValueExtractorPath, PojoValueAdditionalMetadataBuilder> valueBuilders =
			new LinkedHashMap<>();
	private final Map<Class<?>, List<?>> markers = new LinkedHashMap<>();

	PojoPropertyAdditionalMetadataBuilder(PojoTypeAdditionalMetadataBuilder rootBuilder, String propertyName) {
		this.rootBuilder = rootBuilder;
		this.propertyName = propertyName;
	}

	@Override
	public ContextualFailureCollector getFailureCollector() {
		return rootBuilder.getFailureCollector().withContext(
				PojoEventContexts.fromPath( PojoModelPath.fromRoot( propertyName ) )
		);
	}

	@Override
	public PojoAdditionalMetadataCollectorValueNode value(ContainerValueExtractorPath extractorPath) {
		return valueBuilders.computeIfAbsent(
				extractorPath,
				path -> new PojoValueAdditionalMetadataBuilder( rootBuilder, propertyName, extractorPath )
		);
	}

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

	PojoPropertyAdditionalMetadata build() {
		Map<ContainerValueExtractorPath, PojoValueAdditionalMetadata> values = new HashMap<>();
		for ( Map.Entry<ContainerValueExtractorPath, PojoValueAdditionalMetadataBuilder> entry : valueBuilders.entrySet() ) {
			values.put( entry.getKey(), entry.getValue().build() );
		}
		return new PojoPropertyAdditionalMetadata( values, markers );
	}

}
