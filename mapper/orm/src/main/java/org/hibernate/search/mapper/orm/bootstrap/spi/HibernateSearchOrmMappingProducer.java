/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.spi;

import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public interface HibernateSearchOrmMappingProducer {

	Map<Class<?>, JaxbEntityMappingsImpl> produceMappings(
			ConfigurationPropertySource propertySource,
			MetadataBuildingContext buildingContext
	);

}
