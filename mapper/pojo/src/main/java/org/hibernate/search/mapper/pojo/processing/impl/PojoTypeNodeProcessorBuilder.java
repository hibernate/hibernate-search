/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingIndexModelCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMappingContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.IdentifierMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.ReadableProperty;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeNodeProcessorBuilder extends AbstractPojoProcessorBuilder
		implements PojoTypeNodeMappingCollector {

	private final Map<ReadableProperty, PojoPropertyNodeProcessorBuilder> propertyProcessorBuilders = new HashMap<>();

	public PojoTypeNodeProcessorBuilder(
			PojoIntrospector introspector,
			TypeMappingContributorProvider<PojoTypeNodeMappingCollector> contributorProvider,
			Class<?> javaType,
			MappingIndexModelCollector indexModelBuilder,
			IdentifierMappingCollector identifierBridgeCollector) {
		super( introspector, contributorProvider, javaType, indexModelBuilder,
				identifierBridgeCollector);
	}

	@Override
	public PojoPropertyNodeMappingCollector property(String name) {
		ReadableProperty property = introspector.findReadableProperty( javaType, name );
		return property( property );
	}

	@Override
	public PojoPropertyNodeProcessorBuilder property(ReadableProperty property) {
		// TODO handle collection unwrapping?
		return propertyProcessorBuilders.computeIfAbsent( property, this::createPropertyProcessorBuilder );
	}

	private PojoPropertyNodeProcessorBuilder createPropertyProcessorBuilder(ReadableProperty property) {
		// TODO use more advanced reflection here (allow to take advantage of @Timestamp for instance)
		Class<?> nestedType = property.getType();
		return new PojoPropertyNodeProcessorBuilder( introspector, contributorProvider,
				nestedType, indexModelCollector, identifierBridgeCollector, property );
	}

	public PojoTypeNodeProcessor build() {
		return new PojoTypeNodeProcessor( processors, propertyProcessorBuilders.values() );
	}

}
