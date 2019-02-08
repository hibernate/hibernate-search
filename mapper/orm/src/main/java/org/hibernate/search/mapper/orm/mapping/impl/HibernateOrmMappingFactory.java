/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingFactory;

public final class HibernateOrmMappingFactory implements PojoMappingFactory<HibernateOrmMappingPartialBuildState> {

	HibernateOrmMappingFactory() {
	}

	@Override
	public HibernateOrmMappingPartialBuildState createMapping(ConfigurationPropertySource propertySource,
			PojoMappingDelegate mappingDelegate) {
		return new HibernateOrmMappingPartialBuildState( mappingDelegate );
	}
}
