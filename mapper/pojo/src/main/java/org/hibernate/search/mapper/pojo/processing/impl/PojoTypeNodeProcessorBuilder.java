/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeNodeProcessorBuilder extends AbstractPojoProcessorBuilder
		implements PojoTypeNodeMappingCollector {

	private final Map<PropertyHandle, PojoPropertyNodeProcessorBuilder> propertyProcessorBuilders = new HashMap<>();

	public PojoTypeNodeProcessorBuilder(
			PojoPropertyNodeProcessorBuilder parent, PojoTypeModel<?> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		super( parent, typeModel, contributorProvider, indexModelBinder, bindingContext, identityMappingCollector );
	}

	@Override
	public void routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder) {
		RoutingKeyBridge bridge = indexModelBinder.addRoutingKeyBridge( bindingContext, indexableModel, builder );
		identityMappingCollector.routingKeyBridge( bridge );
	}

	@Override
	public PojoPropertyNodeMappingCollector property(PropertyHandle propertyHandle) {
		return propertyProcessorBuilders.computeIfAbsent( propertyHandle, this::createPropertyProcessorBuilder );
	}

	private PojoPropertyNodeProcessorBuilder createPropertyProcessorBuilder(PropertyHandle propertyHandle) {
		return new PojoPropertyNodeProcessorBuilder(
				this, propertyHandle, indexableModel.property( propertyHandle.getName() ).getTypeModel(),
				contributorProvider, indexModelBinder, bindingContext, identityMappingCollector
		);
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "type " ).append( indexableModel.getTypeModel().getJavaClass().getSimpleName() );
	}

	public PojoTypeNodeProcessor build() {
		return new PojoTypeNodeProcessor( bindingContext.getParentIndexObjectAccessors(),
				processors, propertyProcessorBuilders.values() );
	}

}
