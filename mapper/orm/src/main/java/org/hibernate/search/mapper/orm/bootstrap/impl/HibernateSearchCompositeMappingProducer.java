/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateSearchOrmMappingProducer;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

import org.jboss.jandex.IndexView;

@SuppressWarnings("deprecation")
public class HibernateSearchCompositeMappingProducer implements org.hibernate.boot.spi.AdditionalJaxbMappingProducer {

	@Override
	@SuppressForbiddenApis(reason = "Strangely, this SPI involves the internal MappingBinder class,"
			+ " and there's nothing we can do about it")
	public Collection<MappingDocument> produceAdditionalMappings(final MetadataImplementor metadata,
			IndexView jandexIndex, final MappingBinder mappingBinder, final MetadataBuildingContext buildingContext) {
		Optional<HibernateSearchPreIntegrationService> preIntegrationServiceOptional =
				HibernateOrmUtils.getServiceOrEmpty( buildingContext.getBootstrapContext().getServiceRegistry(),
						HibernateSearchPreIntegrationService.class );
		if ( !preIntegrationServiceOptional.isPresent() ) {
			// Hibernate Search is disabled
			return Collections.emptyList();
		}

		HibernateSearchPreIntegrationService preIntegrationService = preIntegrationServiceOptional.get();
		List<MappingDocument> mappings = new ArrayList<>();
		ConfigurationPropertySource propertySource = preIntegrationService.propertySource()
				.withMask( SearchIntegrationEnvironment.CONFIGURATION_PROPERTIES_MASK )
				.withMask( HibernateOrmMapperSettings.Radicals.COORDINATION );
		for ( HibernateSearchOrmMappingProducer mappingProducer : preIntegrationService
				.coordinationStrategyConfiguration().mappingProducers() ) {
			mappings.addAll( mappingProducer.produceMappings( propertySource, metadata.getDatabase().getDialect(),
					mappingBinder, buildingContext ) );
		}
		return mappings;
	}
}
