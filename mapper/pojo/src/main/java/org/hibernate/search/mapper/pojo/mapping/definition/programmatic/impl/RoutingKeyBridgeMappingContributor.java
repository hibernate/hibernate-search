/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;


/**
 * @author Yoann Rodiere
 */
class RoutingKeyBridgeMappingContributor implements PojoTypeMetadataContributor {

	private final BridgeBuilder<? extends RoutingKeyBridge> routingKeyBridgeBuilder;

	RoutingKeyBridgeMappingContributor(BridgeBuilder<? extends RoutingKeyBridge> routingKeyBridgeBuilder) {
		this.routingKeyBridgeBuilder = routingKeyBridgeBuilder;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		collector.routingKeyBridge( routingKeyBridgeBuilder );
	}

}
