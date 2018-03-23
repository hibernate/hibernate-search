/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.augmented.building.spi.PojoAugmentedModelCollectorTypeNode;

final class HibernateOrmEntityTypeMetadataContributor implements PojoTypeMetadataContributor {
	@Override
	public void contributeModel(PojoAugmentedModelCollectorTypeNode collector) {
		collector.markAsEntity();
		/*
		 * TODO register the inverse associations
		 */
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
		/*
		 * TODO register the default identifier mapping? We would need new APIs in the collector, though,
		 * or to allow identifier mapping overrides.
		 */
	}
}
