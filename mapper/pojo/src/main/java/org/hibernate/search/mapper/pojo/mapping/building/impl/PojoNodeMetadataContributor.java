/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;


import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @author Yoann Rodiere
 */
public interface PojoNodeMetadataContributor<CMO extends PojoNodeModelCollector, CMA extends PojoNodeMappingCollector>
		extends TypeMetadataContributor {

	@Override
	default void beforeNestedContributions(MappableTypeModel typeModel) {
		// No-op by default
	}

	void contributeModel(CMO collector);

	void contributeMapping(CMA collector);

}
