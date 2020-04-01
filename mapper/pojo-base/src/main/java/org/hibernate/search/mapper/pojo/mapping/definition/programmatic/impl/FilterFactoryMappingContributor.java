/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.FilterBinder;

class FilterFactoryMappingContributor implements PojoTypeMetadataContributor {

	private final String name;
	private final FilterBinder binder;

	FilterFactoryMappingContributor(String name, FilterBinder binder) {
		this.name = name;
		this.binder = binder;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		collector.filter( name, binder );
	}

}
