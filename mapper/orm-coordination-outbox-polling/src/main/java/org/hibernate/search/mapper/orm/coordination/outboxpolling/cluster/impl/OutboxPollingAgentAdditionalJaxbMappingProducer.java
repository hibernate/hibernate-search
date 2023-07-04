/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl;

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
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.PayloadType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.UuidGenerationStrategy;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.PayloadMappingUtils;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.UuidDataTypeUtils;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.spi.HibernateOrmMapperOutboxPollingSpiSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingAgentAdditionalJaxbMappingProducer
		implements HibernateSearchOrmMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String HIBERNATE_SEARCH = "hibernate-search";

	public static final String CLASS_NAME = Agent.class.getName();

	// Setting both the JPA entity name and the native entity name to the FQCN so that:
	// 1. We don't pollute the namespace of JPA entity names with something like
	// "Agent" that could potentially conflict with user-defined entities.
	// 2. We can still use session methods (persist, ...) without passing the entity name,
	// because our override actually matches the default for the native entity name.
	public static final String ENTITY_NAME = CLASS_NAME;

	private static final String ENTITY_DEFINITION_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<hibernate-mapping schema=\"%1$s\" catalog=\"%2$s\">\n" +
			"    <class name=\"" + CLASS_NAME + "\" entity-name=\"" + ENTITY_NAME + "\" table=\"%3$s\">\n" +
			"        <id name=\"id\" type=\"%5$s\">\n" +
			"            <generator class=\"org.hibernate.id.UUIDGenerator\">\n" +
			"                <param name=\"uuid_gen_strategy_class\">%4$s</param>\n" +
			"            </generator>\n" +
			"        </id>\n" +
			"        <property name=\"type\" nullable=\"false\">\n" +
			"            <type name=\"org.hibernate.type.EnumType\">\n" +
			"                <param name=\"enumClass\">" + AgentType.class.getName() + "</param>\n" +
			"            </type>\n" +
			"        </property>\n" +
			"        <property name=\"name\" nullable=\"false\" />\n" +
			"        <property name=\"expiration\" nullable=\"false\" />\n" +
			"        <property name=\"state\" nullable=\"false\">\n" +
			"            <type name=\"org.hibernate.type.EnumType\">\n" +
			"                <param name=\"enumClass\">" + AgentState.class.getName() + "</param>\n" +
			"            </type>\n" +
			"        </property>\n" +
			"        <property name=\"totalShardCount\" nullable=\"true\" />\n" +
			"        <property name=\"assignedShardIndex\" nullable=\"true\" />\n" +
			// Payload column (reserved for future use):
			"        %6$s\n" +
			"    </class>\n" +
			"</hibernate-mapping>\n";

	@SuppressWarnings("deprecation")
	public static final String ENTITY_DEFINITION = String.format(
			Locale.ROOT, ENTITY_DEFINITION_TEMPLATE, "", "",
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_TABLE,
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY,
			UuidDataTypeUtils.UUID_CHAR,
			PayloadMappingUtils.payload(
					HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_PAYLOAD_TYPE, true )
	);

	private static final OptionalConfigurationProperty<String> AGENT_ENTITY_MAPPING =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSpiSettings.CoordinationRadicals.AGENT_ENTITY_MAPPING )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_AGENT_SCHEMA =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_SCHEMA )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_AGENT_CATALOG =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_CATALOG )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_AGENT_TABLE =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_TABLE )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<UuidGenerationStrategy> ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY )
					.as( UuidGenerationStrategy.class, UuidGenerationStrategy::of )
					.build();

	private static final OptionalConfigurationProperty<String> ENTITY_MAPPING_AGENT_UUID_TYPE =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE )
					.asString()
					.build();

	@SuppressWarnings("deprecation")
	private static final OptionalConfigurationProperty<PayloadType> ENTITY_MAPPING_AGENT_PAYLOAD_TYPE =
			ConfigurationProperty.forKey(
					HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_PAYLOAD_TYPE )
					.as( PayloadType.class, PayloadType::of )
					.build();

	@Override
	@SuppressForbiddenApis(reason = "Strangely, this SPI involves the internal MappingBinder class,"
			+ " and there's nothing we can do about it")
	public Collection<MappingDocument> produceMappings(ConfigurationPropertySource propertySource, Dialect dialect,
			MappingBinder mappingBinder, MetadataBuildingContext buildingContext) {

		Optional<String> mapping = AGENT_ENTITY_MAPPING.get( propertySource );
		Optional<String> schema = ENTITY_MAPPING_AGENT_SCHEMA.get( propertySource );
		Optional<String> catalog = ENTITY_MAPPING_AGENT_CATALOG.get( propertySource );
		Optional<String> table = ENTITY_MAPPING_AGENT_TABLE.get( propertySource );
		Optional<UuidGenerationStrategy> uuidStrategy = ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY.get( propertySource );
		Optional<String> uuidType = ENTITY_MAPPING_AGENT_UUID_TYPE.get( propertySource );
		Optional<PayloadType> payloadType = ENTITY_MAPPING_AGENT_PAYLOAD_TYPE.get( propertySource );

		// only allow configuring the entire mapping or table/catalog/schema/generator/datatype names
		if ( mapping.isPresent()
				&& ( schema.isPresent()
						|| catalog.isPresent() || table.isPresent() || uuidStrategy.isPresent() || uuidType.isPresent()
						|| payloadType.isPresent() ) ) {
			throw log.agentConfigurationPropertyConflict(
					AGENT_ENTITY_MAPPING.resolveOrRaw( propertySource ),
					new String[] {
							ENTITY_MAPPING_AGENT_SCHEMA.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_AGENT_CATALOG.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_AGENT_TABLE.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_AGENT_UUID_TYPE.resolveOrRaw( propertySource ),
							ENTITY_MAPPING_AGENT_PAYLOAD_TYPE.resolveOrRaw( propertySource )
					}
			);
		}
		if ( payloadType.isPresent() ) {
			log.usingDeprecatedPayloadTypeConfigurationProperty(
					ENTITY_MAPPING_AGENT_PAYLOAD_TYPE.resolveOrRaw( propertySource ) );
		}

		String resolvedUuidType = UuidDataTypeUtils.uuidType(
				uuidType.orElse( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE ),
				dialect );

		@SuppressWarnings("deprecation")
		String entityDefinition = mapping.orElseGet( () -> String.format(
				Locale.ROOT,
				ENTITY_DEFINITION_TEMPLATE,
				schema.orElse( "" ),
				catalog.orElse( "" ),
				table.orElse( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_TABLE ),
				uuidStrategy.orElse(
						HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY )
						.strategy(),
				resolvedUuidType,
				PayloadMappingUtils.payload(
						payloadType.orElse(
								HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_PAYLOAD_TYPE ),
						true
				)
		) );

		log.agentGeneratedEntityMapping( entityDefinition );
		Origin origin = new Origin( SourceType.OTHER, "search" );

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( entityDefinition.getBytes() );
		BufferedInputStream bufferedInputStream = new BufferedInputStream( byteArrayInputStream );
		Binding<?> binding = mappingBinder.bind( bufferedInputStream, origin );

		JaxbHbmHibernateMapping root = (JaxbHbmHibernateMapping) binding.getRoot();

		MappingDocument mappingDocument = new MappingDocument( HIBERNATE_SEARCH, root, origin, buildingContext );
		return Collections.singletonList( mappingDocument );
	}
}
