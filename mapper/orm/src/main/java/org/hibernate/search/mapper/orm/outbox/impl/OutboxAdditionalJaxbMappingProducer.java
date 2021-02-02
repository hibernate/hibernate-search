/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;

@SuppressWarnings( "deprecation" )
public class OutboxAdditionalJaxbMappingProducer implements org.hibernate.boot.spi.AdditionalJaxbMappingProducer {

	private static final ConfigurationProperty<Boolean> FILL_OUTBOX_TABLE =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.FILL_OUTBOX_TABLE )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.FILL_OUTBOX_TABLE )
					.build();

	@Override
	public Collection<MappingDocument> produceAdditionalMappings(final MetadataImplementor metadata,
			IndexView jandexIndex, final MappingBinder mappingBinder, final MetadataBuildingContext buildingContext) {
		final ServiceRegistry serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		ConfigurationService service = serviceRegistry.getService( ConfigurationService.class );
		Boolean outboxEnabled = FILL_OUTBOX_TABLE.get( ConfigurationPropertySource.fromMap( service.getSettings() ) );

		if ( !outboxEnabled ) {
			return Collections.emptyList();
		}

		// TODO HSEARCH-4132 Register synthetic entities
		return Collections.emptyList();
	}
}
