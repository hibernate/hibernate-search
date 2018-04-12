/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;

final class HibernateOrmEntityTypeMetadataContributor implements PojoTypeMetadataContributor {
	private final List<PojoTypeMetadataContributor> delegates;

	HibernateOrmEntityTypeMetadataContributor(List<PojoTypeMetadataContributor> delegates) {
		this.delegates = delegates;
	}

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorTypeNode collector) {
		collector.markAsEntity();
		for ( PojoTypeMetadataContributor delegate : delegates ) {
			delegate.contributeModel( collector );
		}
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		for ( PojoTypeMetadataContributor delegate : delegates ) {
			delegate.contributeMapping( collector );
		}
		/*
		 * TODO register the default identifier mapping? We would need new APIs in the collector, though,
		 * or to allow identifier mapping overrides.
		 */
	}
}
