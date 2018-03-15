/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedModelCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorPropertyNode;


/**
 * @author Yoann Rodiere
 */
public class PropertyBridgeMappingContributor
		implements PojoMetadataContributor<PojoAugmentedModelCollector, PojoMappingCollectorPropertyNode> {

	private final BridgeBuilder<? extends PropertyBridge> bridgeBuilder;

	PropertyBridgeMappingContributor(BridgeBuilder<? extends PropertyBridge> bridgeBuilder) {
		this.bridgeBuilder = bridgeBuilder;
	}

	@Override
	public void contributeModel(PojoAugmentedModelCollector collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		collector.bridge(
				bridgeBuilder
				/*
				 * Ignore mapped types, we don't need to discover new mappings automatically
				 * like in the annotation mappings.
				 */
		);
	}

}
