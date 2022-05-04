/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateSearchOrmMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.spi.HibernateOrmMapperOutboxPollingSpiSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class OutboxPollingOutboxEventAdditionalJaxbMappingProducer
		implements HibernateSearchOrmMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String CLASS_NAME = OutboxEvent.class.getName();

	// Setting both the JPA entity name and the native entity name to the FQCN so that:
	// 1. We don't pollute the namespace of JPA entity names with something like
	// "OutboxEvent" that could potentially conflict with user-defined entities.
	// 2. We can still use session methods (persist, ...) without passing the entity name,
	// because our override actually matches the default for the native entity name.
	public static final String ENTITY_NAME = CLASS_NAME;

	public static final String ENTITY_DEFINITION_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<hibernate-mapping schema=\"%1$s\" catalog=\"%2$s\">\n" +
			"    <class name=\"" + CLASS_NAME + "\" entity-name=\"" + ENTITY_NAME + "\" table=\"%3$s\">\n" +
			"        <id name=\"id\" type=\"long\">\n" +
			"            <generator class=\"org.hibernate.id.enhanced.SequenceStyleGenerator\">\n" +
			"                <param name=\"sequence_name\">%4$s</param>\n" +
			"                <param name=\"table_name\">%4$s</param>\n" +
			"                <param name=\"initial_value\">1</param>\n" +
			"                <param name=\"increment_size\">1</param>\n" +
			"            </generator>\n" +
			"        </id>\n" +
			"        <property name=\"entityName\" type=\"string\" length=\"256\" nullable=\"false\" />\n" +
			"        <property name=\"entityId\" type=\"string\" length=\"256\" nullable=\"false\" />\n" +
			"        <property name=\"entityIdHash\" type=\"integer\" index=\"entityIdHash\" nullable=\"false\" />\n" +
			"        <property name=\"payload\" type=\"materialized_blob\" nullable=\"false\" />\n" +
			"        <property name=\"retries\" type=\"integer\" nullable=\"false\" />\n" +
			"        <property name=\"processAfter\" type=\"Instant\" index=\"processAfter\" nullable=\"true\" />\n" +
			"        <property name=\"status\" index=\"status\" nullable=\"false\">\n" +
			"            <type name=\"org.hibernate.type.EnumType\">\n" +
			"                <param name=\"enumClass\">" + OutboxEvent.Status.class.getName() + "</param>\n" +
			"            </type>\n" +
			"        </property>\n" +
			"    </class>\n" +
			"</hibernate-mapping>\n";

	public static final String ENTITY_DEFINITION = String.format(
			Locale.ROOT, ENTITY_DEFINITION_TEMPLATE, "", "", HibernateOrmMapperOutboxPollingSettings.Defaults.ENTITY_MAPPING_OUTBOX_EVENT_TABLE, HibernateOrmMapperOutboxPollingSettings.Defaults.ENTITY_MAPPING_OUTBOX_EVENT_GENERATOR
	);

	private static final OptionalConfigurationProperty<String> OUTBOXEVENT_ENTITY_MAPPING =
			ConfigurationProperty.forKey(
							HibernateOrmMapperOutboxPollingSpiSettings.CoordinationRadicals.OUTBOXEVENT_ENTITY_MAPPING )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_SCHEMA =
			ConfigurationProperty.forKey(
							HibernateOrmMapperOutboxPollingSettings.OutboxPollingEntityMappingRadicals.ENTITY_MAPPING_OUTBOXEVENT_SCHEMA )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_CATALOG =
			ConfigurationProperty.forKey(
							HibernateOrmMapperOutboxPollingSettings.OutboxPollingEntityMappingRadicals.ENTITY_MAPPING_OUTBOXEVENT_CATALOG )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_TABLE =
			ConfigurationProperty.forKey(
							HibernateOrmMapperOutboxPollingSettings.OutboxPollingEntityMappingRadicals.ENTITY_MAPPING_OUTBOXEVENT_TABLE )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_GENERATOR =
			ConfigurationProperty.forKey(
							HibernateOrmMapperOutboxPollingSettings.OutboxPollingEntityMappingRadicals.ENTITY_MAPPING_OUTBOXEVENT_GENERATOR )
					.asString()
					.build();

	@Override
	@SuppressForbiddenApis(reason = "Strangely, this SPI involves the internal MappingBinder class,"
			+ " and there's nothing we can do about it")
	public Collection<MappingDocument> produceMappings(ConfigurationPropertySource propertySource,
			MappingBinder mappingBinder, MetadataBuildingContext buildingContext) {

		Optional<String> mapping = OUTBOXEVENT_ENTITY_MAPPING.get( propertySource );
		Optional<String> schema = ENTITY_MAPPING_OUTBOXEVENT_SCHEMA.get( propertySource );
		Optional<String> catalog = ENTITY_MAPPING_OUTBOXEVENT_CATALOG.get( propertySource );
		Optional<String> table = ENTITY_MAPPING_OUTBOXEVENT_TABLE.get( propertySource );
		Optional<String> generator = ENTITY_MAPPING_OUTBOXEVENT_GENERATOR.get( propertySource );

		// only allow configuring the entire mapping or table/catalog/schema/generator names
		if ( mapping.isPresent() && ( schema.isPresent() || catalog.isPresent() || table.isPresent() || generator.isPresent() ) ) {
			throw log.outboxEventConfigurationPropertyConflict(
					OUTBOXEVENT_ENTITY_MAPPING.resolveOrRaw( propertySource ),
					new String[] {
							ENTITY_MAPPING_OUTBOXEVENT_SCHEMA.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_CATALOG.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_TABLE.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_GENERATOR.resolveOrRaw( propertySource )
					}
			);
		}

		String entityDefinition = mapping.orElseGet( () ->
				String.format(
						Locale.ROOT,
						ENTITY_DEFINITION_TEMPLATE,
						schema.orElse( "" ),
						catalog.orElse( "" ),
						table.orElse( HibernateOrmMapperOutboxPollingSettings.Defaults.ENTITY_MAPPING_OUTBOX_EVENT_TABLE ),
						generator.orElse( HibernateOrmMapperOutboxPollingSettings.Defaults.ENTITY_MAPPING_OUTBOX_EVENT_GENERATOR )

				)
		);

		log.outboxEventGeneratedEntityMapping( entityDefinition );
		Origin origin = new Origin( SourceType.OTHER, "search" );

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( entityDefinition.getBytes() );
		BufferedInputStream bufferedInputStream = new BufferedInputStream( byteArrayInputStream );
		Binding<?> binding = mappingBinder.bind( bufferedInputStream, origin );

		JaxbHbmHibernateMapping root = (JaxbHbmHibernateMapping) binding.getRoot();

		MappingDocument mappingDocument = new MappingDocument( root, origin, buildingContext );
		return Collections.singletonList( mappingDocument );
	}
}
