/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;

@SuppressWarnings("deprecation")
public class OutboxAdditionalJaxbMappingProducer implements org.hibernate.boot.spi.AdditionalJaxbMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String OUTBOX_TABLE_NAME = "HIBERNATE_SEARCH_OUTBOX_TABLE";

	private static final String OUTBOX_ENTITY_DEFINITION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	"\n" +
	"<hibernate-mapping>\n" +
	"    <class name=\"" + OutboxEvent.class.getName() + "\" table=\"" + OUTBOX_TABLE_NAME + "\">\n" +
	"        <id name=\"id\" column=\"ID\" type=\"integer\">\n" +
	"            <generator class=\"org.hibernate.id.enhanced.SequenceStyleGenerator\">\n" +
	"                <param name=\"sequence_name\">" + OUTBOX_TABLE_NAME + "_GENERATOR</param>\n" +
	"                <param name=\"table_name\">" + OUTBOX_TABLE_NAME + "_GENERATOR</param>\n" +
	"                <param name=\"initial_value\">1</param>\n" +
	"                <param name=\"increment_size\">1</param>\n" +
	"            </generator>\n" +
	"        </id>\n" +
	"		 <property name=\"type\" >\n" +
	" 			 <type name=\"org.hibernate.type.EnumType\">\n" +
	" 				 <param name=\"enumClass\">" + OutboxEvent.Type.class.getName() + "</param>\n" +
	" 			 </type>\n" +
	"		 </property>\n" +
	"        <property name=\"entityName\" type=\"string\" />\n" +
	"        <property name=\"entityId\" type=\"string\" />\n" +
	"        <property name=\"payload\" type=\"binary\" length=\"8192\" />\n" +
	"        <property name=\"retries\" type=\"integer\" />\n" +
	"    </class>\n" +
	"</hibernate-mapping>\n";

	@Override
	public Collection<MappingDocument> produceAdditionalMappings(final MetadataImplementor metadata,
			IndexView jandexIndex, final MappingBinder mappingBinder, final MetadataBuildingContext buildingContext) {
		ServiceRegistry serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		ConfigurationService service = serviceRegistry.getService( ConfigurationService.class );

		Object customIndexingStrategy = service.getSettings().get( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY );
		if ( !AutomaticIndexingStrategyNames.OUTBOX_POLLING.equals( customIndexingStrategy ) ) {
			return Collections.emptyList();
		}

		log.outboxGeneratedEntityMapping( OUTBOX_ENTITY_DEFINITION );
		Origin origin = new Origin( SourceType.OTHER, "search" );

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( OUTBOX_ENTITY_DEFINITION.getBytes() );
		BufferedInputStream bufferedInputStream = new BufferedInputStream( byteArrayInputStream );
		Binding binding = mappingBinder.bind( bufferedInputStream, origin );

		JaxbHbmHibernateMapping root = (JaxbHbmHibernateMapping) binding.getRoot();

		MappingDocument mappingDocument = new MappingDocument( root, origin, buildingContext );
		return Collections.singletonList( mappingDocument );
	}
}
