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
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

import org.jboss.jandex.IndexView;

@SuppressWarnings("deprecation")
public class HibernateSearchAdditionalJaxbMappingProducer implements org.hibernate.boot.spi.AdditionalJaxbMappingProducer {

	@Override
	@SuppressForbiddenApis(reason = "Strangely, this SPI involves the internal MappingBinder class,"
			+ " and there's nothing we can do about it")
	public Collection<MappingDocument> produceAdditionalMappings(final MetadataImplementor metadata,
			IndexView jandexIndex, final MappingBinder mappingBinder, final MetadataBuildingContext buildingContext) {
		Optional<HibernateSearchPreIntegrationService> preIntegrationService =
				HibernateOrmUtils.getServiceOrEmpty( buildingContext.getBootstrapContext().getServiceRegistry(),
						HibernateSearchPreIntegrationService.class );
		if ( !preIntegrationService.isPresent() ) {
			// Hibernate Search is disabled
			return Collections.emptyList();
		}
		List<MappingDocument> mappings = new ArrayList<>();
		for ( org.hibernate.boot.spi.AdditionalJaxbMappingProducer mappingProducer : preIntegrationService.get()
				.coordinationStrategyConfiguration().mappingProducers() ) {
			mappings.addAll( mappingProducer.produceAdditionalMappings( metadata, jandexIndex, mappingBinder,
					buildingContext ) );
		}
		return mappings;
	}
}
