/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;

class TypeBridgeMappingContributor implements PojoTypeMetadataContributor {

	private final TypeBinder binder;
	private final Map<String, Object> params;

	TypeBridgeMappingContributor(TypeBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorTypeNode collector) {
		collector.typeBinder( binder, params );
	}

}
