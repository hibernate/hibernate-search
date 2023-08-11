/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl;

import static org.hibernate.search.mapper.orm.coordination.outboxpolling.impl.HibernateOrmUtils.isDiscriminatorMultiTenancyEnabled;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl.JaxbMappingHelper.marshall;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl.JaxbMappingHelper.unmarshall;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.Length;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
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
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl.AdditionalMappingBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.type.SqlTypes;

public class OutboxPollingAgentAdditionalJaxbMappingProducer implements HibernateSearchOrmMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String CLASS_NAME = Agent.class.getName();

	// Setting both the JPA entity name and the native entity name to the FQCN so that:
	// 1. We don't pollute the namespace of JPA entity names with something like
	// "Agent" that could potentially conflict with user-defined entities.
	// 2. We can still use session methods (persist, ...) without passing the entity name,
	// because our override actually matches the default for the native entity name.
	public static final String ENTITY_NAME = CLASS_NAME;

	@SuppressWarnings("deprecation")
	public static final String ENTITY_DEFINITION = marshall( createMappings( "", "",
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_TABLE,
			SqlTypes.CHAR, PayloadMappingUtils.payload(
					HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_PAYLOAD_TYPE ),
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY.strategy(),
			false
	) );

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
	public Map<Class<?>, JaxbEntityMappings> produceMappings(ConfigurationPropertySource propertySource, Dialect dialect,
			MetadataBuildingContext buildingContext) {
		Optional<String> mapping = AGENT_ENTITY_MAPPING.get( propertySource );
		Optional<String> schema = ENTITY_MAPPING_AGENT_SCHEMA.get( propertySource );
		Optional<String> catalog = ENTITY_MAPPING_AGENT_CATALOG.get( propertySource );
		Optional<String> table = ENTITY_MAPPING_AGENT_TABLE.get( propertySource );

		Optional<UuidGenerationStrategy> uuidStrategy = ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY.get( propertySource );
		Optional<Integer> uuidType = ENTITY_MAPPING_AGENT_UUID_TYPE.getAndMap( propertySource,
				value -> UuidDataTypeUtils.uuidType(
						value,
						propertySource,
						ENTITY_MAPPING_AGENT_UUID_TYPE,
						dialect
				) );
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

		JaxbEntityMappings mappings;
		if ( mapping.isPresent() ) {
			mappings = unmarshall( mapping.get() );
		}
		else {
			int resolvedUuidType = uuidType.orElseGet( () -> UuidDataTypeUtils.defaultUuidType( dialect ) );
			@SuppressWarnings("deprecation")
			int resolvedPayloadType = PayloadMappingUtils.payload(
					payloadType.orElse(
							HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_PAYLOAD_TYPE )
			);
			String resolvedUuidStrategy = uuidStrategy.orElse(
					HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY )
					.strategy();

			mappings = createMappings(
					schema.orElse( "" ),
					catalog.orElse( "" ),
					table.orElse( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_TABLE ),
					resolvedUuidType, resolvedPayloadType, resolvedUuidStrategy,
					isDiscriminatorMultiTenancyEnabled( buildingContext )
			);
		}

		log.agentGeneratedEntityMapping( mappings );

		return Map.of( Agent.class, mappings );
	}

	private static JaxbEntityMappings createMappings(String schema, String catalog,
			String table, int resolvedUuidType, int resolvedPayloadType, String resolvedUuidStrategy,
			boolean tenantIdRequired) {
		AdditionalMappingBuilder builder = new AdditionalMappingBuilder(
				Agent.class, ENTITY_NAME )
				.id( resolvedUuidType, resolvedUuidStrategy )
				.table( schema, catalog, table )
				.enumAttribute( "type", null, false )
				.attribute( "name", null, false )
				.attribute( "expiration", null, false )
				.enumAttribute( "state", null, false )
				.attribute( "totalShardCount", null, true )
				.attribute( "assignedShardIndex", null, true )
				.attribute( "payload", Length.LONG32, true, resolvedPayloadType );
		if ( tenantIdRequired ) {
			builder.tenantId( "tenantId" );
		}
		return builder.build();
	}
}
