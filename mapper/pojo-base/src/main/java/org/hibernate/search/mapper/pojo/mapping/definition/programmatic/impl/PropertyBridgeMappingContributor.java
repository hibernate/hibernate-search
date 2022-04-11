/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;



class PropertyBridgeMappingContributor implements PojoPropertyMetadataContributor {

	private final PropertyBinder binder;
	private final Map<String, Object> params;

	PropertyBridgeMappingContributor(PropertyBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		collector.propertyBinder(
				binder,
				/*
				 * Ignore mapped types, we don't need to discover new mappings automatically
				 * like in the annotation mappings.
				 */
				params
		);
	}

}
