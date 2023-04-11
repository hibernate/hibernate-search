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
import org.hibernate.dialect.Dialect;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateSearchOrmMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.UuidGenerationStrategy;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.UuidDataTypeUtils;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.spi.HibernateOrmMapperOutboxPollingSpiSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class OutboxPollingOutboxEventAdditionalJaxbMappingProducer
		implements HibernateSearchOrmMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String HIBERNATE_SEARCH = OutboxPollingAgentAdditionalJaxbMappingProducer.HIBERNATE_SEARCH;

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
			"        <id name=\"id\" type=\"%5$s\">\n" +
			"            <generator class=\"org.hibernate.id.UUIDGenerator\">\n" +
			"                <param name=\"uuid_gen_strategy_class\">%4$s</param>\n" +
			"            </generator>\n" +
			"        </id>\n" +
			"        <property name=\"entityName\" type=\"string\" length=\"256\" nullable=\"false\" />\n" +
			"        <property name=\"entityId\" type=\"string\" length=\"256\" nullable=\"false\" />\n" +
			"        <property name=\"entityIdHash\" type=\"integer\" index=\"entityIdHash\" nullable=\"false\" />\n" +
			"        <property name=\"payload\" type=\"materialized_blob\" nullable=\"false\">\n" +
			// HSEARCH-4727: this column length will be ignored in most dialects, since the blob type is normally unbounded,
			// but it will force Hibernate ORM to simulate an unbounded BLOB type with DB2.
			// Using 2147483647 as it's the documented maximum length of BLOBs in DB2:
			// https://www.ibm.com/docs/en/db2-for-zos/11?topic=types-large-objects-lobs
			// TODO HSEARCH-4395/HSEARCH-4532 drop this length definition with ORM 6, because ORM 6 will ignore it.
			"                <column length=\"2147483647\" />\n" +
			"        </property>\n" +
			"        <property name=\"retries\" type=\"integer\" nullable=\"false\" />\n" +
			"        <property name=\"processAfter\" type=\"Instant\" index=\"processAfter\" nullable=\"false\" />\n" +
			"        <property name=\"status\" index=\"status\" nullable=\"false\">\n" +
			"            <type name=\"org.hibernate.type.EnumType\">\n" +
			"                <param name=\"enumClass\">" + OutboxEvent.Status.class.getName() + "</param>\n" +
			"            </type>\n" +
			"        </property>\n" +
			"    </class>\n" +
			"</hibernate-mapping>\n";

	public static final String ENTITY_DEFINITION = String.format(
			Locale.ROOT, ENTITY_DEFINITION_TEMPLATE, "", "",
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE,
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_GEN_STRATEGY,
			UuidDataTypeUtils.UUID_CHAR
	);

	private static final OptionalConfigurationProperty<String> OUTBOXEVENT_ENTITY_MAPPING =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSpiSettings.CoordinationRadicals.OUTBOXEVENT_ENTITY_MAPPING )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_SCHEMA =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_SCHEMA )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_CATALOG =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_CATALOG )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_TABLE =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_TABLE )
					.asString()
					.build();

	public static final OptionalConfigurationProperty<UuidGenerationStrategy> ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY )
					.as( UuidGenerationStrategy.class, UuidGenerationStrategy::of )
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE )
					.asString()
					.build();

	@Override
	@SuppressForbiddenApis(reason = "Strangely, this SPI involves the internal MappingBinder class,"
			+ " and there's nothing we can do about it")
	public Collection<MappingDocument> produceMappings(ConfigurationPropertySource propertySource, Dialect dialect,
			MappingBinder mappingBinder, MetadataBuildingContext buildingContext) {

		Optional<String> mapping = OUTBOXEVENT_ENTITY_MAPPING.get( propertySource );
		Optional<String> schema = ENTITY_MAPPING_OUTBOXEVENT_SCHEMA.get( propertySource );
		Optional<String> catalog = ENTITY_MAPPING_OUTBOXEVENT_CATALOG.get( propertySource );
		Optional<String> table = ENTITY_MAPPING_OUTBOXEVENT_TABLE.get( propertySource );
		Optional<UuidGenerationStrategy> uuidStrategy = ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY.get( propertySource );
		Optional<String> uuidType = ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE.get( propertySource );

		// only allow configuring the entire mapping or table/catalog/schema/generator/datatype names
		if ( mapping.isPresent()
				&& ( schema.isPresent()
						|| catalog.isPresent() || table.isPresent() || uuidStrategy.isPresent() || uuidType.isPresent() ) ) {
			throw log.outboxEventConfigurationPropertyConflict(
					OUTBOXEVENT_ENTITY_MAPPING.resolveOrRaw( propertySource ),
					new String[] {
							ENTITY_MAPPING_OUTBOXEVENT_SCHEMA.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_CATALOG.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_TABLE.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE.resolveOrRaw( propertySource )
					}
			);
		}

		String resolvedUuidType = UuidDataTypeUtils.uuidType(
				uuidType.orElse(
						HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_TYPE ),
				dialect );

		String entityDefinition = mapping.orElseGet( () -> String.format(
				Locale.ROOT,
				ENTITY_DEFINITION_TEMPLATE,
				schema.orElse( "" ),
				catalog.orElse( "" ),
				table.orElse( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE ),
				uuidStrategy.orElse(
						HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_GEN_STRATEGY )
						.strategy(),
				resolvedUuidType
		)
		);

		log.outboxEventGeneratedEntityMapping( entityDefinition );
		Origin origin = new Origin( SourceType.OTHER, HIBERNATE_SEARCH );

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( entityDefinition.getBytes() );
		BufferedInputStream bufferedInputStream = new BufferedInputStream( byteArrayInputStream );
		Binding<?> binding = mappingBinder.bind( bufferedInputStream, origin );

		JaxbHbmHibernateMapping root = (JaxbHbmHibernateMapping) binding.getRoot();

		MappingDocument mappingDocument = new MappingDocument( HIBERNATE_SEARCH, root, origin, buildingContext );
		return Collections.singletonList( mappingDocument );
	}
}
