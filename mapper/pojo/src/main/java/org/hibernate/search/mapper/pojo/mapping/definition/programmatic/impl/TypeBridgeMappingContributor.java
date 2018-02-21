/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;


/**
 * @author Yoann Rodiere
 */
public class TypeBridgeMappingContributor
		implements PojoNodeMetadataContributor<PojoNodeModelCollector, PojoTypeNodeMappingCollector> {

	private final BridgeBuilder<? extends TypeBridge> bridgeBuilder;

	TypeBridgeMappingContributor(BridgeBuilder<? extends TypeBridge> bridgeBuilder) {
		this.bridgeBuilder = bridgeBuilder;
	}

	@Override
	public void contributeModel(PojoNodeModelCollector collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoTypeNodeMappingCollector collector) {
		collector.bridge( bridgeBuilder );
	}

}
