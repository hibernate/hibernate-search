/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;



class MarkerMappingContributor implements PojoPropertyMetadataContributor {

	private final MarkerBinder binder;
	private final Map<String, Object> params;

	MarkerMappingContributor(MarkerBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		collector.markerBinder( binder, params );
	}

}
