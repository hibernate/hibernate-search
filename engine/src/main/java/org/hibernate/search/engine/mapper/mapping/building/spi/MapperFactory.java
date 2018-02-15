/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;

/**
 * @author Yoann Rodiere
 */
public interface MapperFactory<C, M extends MappingImplementor>
		extends MappingKey<M> {

	Mapper<C, M> createMapper(BuildContext buildContext, ConfigurationPropertySource propertySource);

}
