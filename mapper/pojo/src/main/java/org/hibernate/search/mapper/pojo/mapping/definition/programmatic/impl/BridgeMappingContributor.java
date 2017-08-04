/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMappingCollector;


/**
 * @author Yoann Rodiere
 */
public class BridgeMappingContributor
		implements TypeMappingContributor<PojoNodeMappingCollector> {

	private final BridgeDefinition<?> definition;

	public BridgeMappingContributor(BridgeDefinition<?> definition) {
		this.definition = definition;
	}

	@Override
	public void contribute(PojoNodeMappingCollector collector) {
		collector.bridge( definition );
	}

}
