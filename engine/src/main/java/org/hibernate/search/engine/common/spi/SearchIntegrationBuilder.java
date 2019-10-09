/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;


public interface SearchIntegrationBuilder {

	ConfigurationPropertySource getMaskedPropertySource();

	SearchIntegrationBuilder setClassResolver(ClassResolver classResolver);

	SearchIntegrationBuilder setResourceResolver(ResourceResolver resourceResolver);

	SearchIntegrationBuilder setServiceResolver(ServiceResolver serviceResolver);

	SearchIntegrationBuilder setBeanProvider(BeanProvider beanProvider);

	<PBM extends MappingPartialBuildState> SearchIntegrationBuilder addMappingInitiator(
			MappingKey<PBM, ?> mappingKey, MappingInitiator<?, PBM> initiator);

	SearchIntegrationPartialBuildState prepareBuild();

}
