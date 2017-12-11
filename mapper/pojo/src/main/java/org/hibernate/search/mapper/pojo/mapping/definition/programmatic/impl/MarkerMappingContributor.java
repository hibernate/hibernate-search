/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerDefinition;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeModelCollector;


/**
 * @author Yoann Rodiere
 */
public class MarkerMappingContributor
		implements PojoNodeMetadataContributor<PojoPropertyNodeModelCollector, PojoNodeMappingCollector> {

	private final MarkerDefinition<?> definition;

	public MarkerMappingContributor(MarkerDefinition<?> definition) {
		this.definition = definition;
	}

	@Override
	public void contributeModel(PojoPropertyNodeModelCollector collector) {
		collector.marker( definition );
	}

	@Override
	public void contributeMapping(PojoNodeMappingCollector collector) {
		// Nothing to do
	}

}
