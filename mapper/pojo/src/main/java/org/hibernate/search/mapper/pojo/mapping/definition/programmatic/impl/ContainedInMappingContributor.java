/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;


/**
 * @author Yoann Rodiere
 */
public class ContainedInMappingContributor
		implements PojoNodeMetadataContributor<PojoPropertyNodeMappingCollector> {

	@Override
	public void contributeMapping(PojoPropertyNodeMappingCollector collector) {
		collector.containedIn();
	}

}
