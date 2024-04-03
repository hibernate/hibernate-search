/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.Length;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateSearchOrmMappingProducer;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.UuidGenerationStrategy;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.UuidDataTypeUtils;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.spi.HibernateOrmMapperOutboxPollingSpiSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.impl.AdditionalMappingBuilder;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.impl.JaxbMappingHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class OutboxPollingOutboxEventAdditionalMappingProducer
		implements HibernateSearchOrmMappingProducer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String CLASS_NAME = OutboxEvent.class.getName();

	// Setting both the JPA entity name and the native entity name to the FQCN so that:
	// 1. We don't pollute the namespace of JPA entity names with something like
	// "OutboxEvent" that could potentially conflict with user-defined entities.
	// 2. We can still use session methods (persist, ...) without passing the entity name,
	// because our override actually matches the default for the native entity name.
	public static final String ENTITY_NAME = CLASS_NAME;

	public static final String ENTITY_DEFINITION = "<entity-mappings xmlns=\"http://www.hibernate.org/xsd/orm/mapping\">\n"
			+ "    <entity name=\"org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent\" class=\"org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent\" access=\"FIELD\">\n"
			+ "        <table name=\"HSEARCH_OUTBOX_EVENT\" catalog=\"\" schema=\"\">\n"
			+ "            <index name=\"entityIdHash\" column-list=\"entityIdHash\"/>\n"
			+ "            <index name=\"status\" column-list=\"status\"/>\n"
			+ "            <index name=\"processAfter\" column-list=\"processAfter\"/>\n"
			+ "        </table>\n"
			+ "        <attributes>\n"
			+ "            <id name=\"id\">\n"
			+ "                <uuid-generator style=\"random\"/>\n"
			+ "            </id>\n"
			+ "            <basic name=\"entityName\">\n"
			+ "                <column name=\"entityName\" nullable=\"false\" length=\"256\"/>\n"
			+ "            </basic>\n"
			+ "            <basic name=\"entityId\">\n"
			+ "                <column name=\"entityId\" nullable=\"false\" length=\"256\"/>\n"
			+ "            </basic>\n"
			+ "            <basic name=\"entityIdHash\">\n"
			+ "                <column name=\"entityIdHash\" nullable=\"false\"/>\n"
			+ "            </basic>\n"
			+ "            <basic name=\"payload\">\n"
			+ "                <column name=\"payload\" nullable=\"false\" length=\"2147483647\"/>\n"
			+ "            </basic>\n"
			+ "            <basic name=\"retries\">\n"
			+ "                <column name=\"retries\" nullable=\"false\"/>\n"
			+ "            </basic>\n"
			+ "            <basic name=\"processAfter\">\n"
			+ "                <column name=\"processAfter\" nullable=\"false\"/>\n"
			+ "            </basic>\n"
			+ "            <basic name=\"status\">\n"
			+ "                <column name=\"status\" nullable=\"false\"/>\n"
			+ "                <enumerated>STRING</enumerated>\n"
			+ "            </basic>\n"
			+ "        </attributes>\n"
			+ "    </entity>\n"
			+ "</entity-mappings>";

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
	public Consumer<AdditionalMappingContributions> produceMappingContributor(ConfigurationPropertySource propertySource,
			MetadataBuildingContext buildingContext) {
		Optional<String> mapping = OUTBOXEVENT_ENTITY_MAPPING.get( propertySource );
		Optional<String> schema = ENTITY_MAPPING_OUTBOXEVENT_SCHEMA.get( propertySource );
		Optional<String> catalog = ENTITY_MAPPING_OUTBOXEVENT_CATALOG.get( propertySource );
		Optional<String> table = ENTITY_MAPPING_OUTBOXEVENT_TABLE.get( propertySource );

		Optional<UuidGenerationStrategy> uuidStrategy = ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY.get( propertySource );
		Optional<Integer> uuidType = ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE.getAndMap( propertySource,
				value -> UuidDataTypeUtils.uuidType(
						value,
						propertySource,
						ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE
				) );

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

		if ( mapping.isPresent() ) {
			JaxbEntityMappingsImpl mappings = JaxbMappingHelper.unmarshall( mapping.get() );
			log.outboxEventGeneratedEntityMapping( mappings );
			return contributions -> {
				contributions.contributeEntity( Agent.class );
				contributions.contributeBinding( mappings );
			};
		}
		else {
			Integer resolvedUuidType = uuidType.orElse( null );
			String resolvedUuidStrategy = uuidStrategy.orElse(
					HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_GEN_STRATEGY )
					.strategy();
			ClassDetails mappings = createMappings(
					buildingContext,
					schema.orElse( "" ),
					catalog.orElse( "" ),
					table.orElse(
							HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE ),
					resolvedUuidType, resolvedUuidStrategy,
					HibernateOrmUtils.isDiscriminatorMultiTenancyEnabled( buildingContext )
			);

			log.outboxEventGeneratedEntityMappingClassDetails( mappings );

			return contributions -> {
				contributions.contributeEntity( Agent.class );
				contributions.contributeManagedClass( mappings );
			};
		}
	}

	private static ClassDetails createMappings(MetadataBuildingContext buildingContext, String schema, String catalog,
			String table, Integer resolvedUuidType, String resolvedUuidStrategy,
			boolean tenantIdRequired) {
		AdditionalMappingBuilder builder = new AdditionalMappingBuilder(
				buildingContext, OutboxEvent.class, ENTITY_NAME )
				.id( resolvedUuidType, resolvedUuidStrategy )
				.index( "entityIdHash" )
				.index( "status" )
				.index( "processAfter" )
				.table( schema, catalog, table )
				.attribute( "entityName", 256, false )
				.attribute( "entityId", 256, false )
				.attribute( "entityIdHash", null, false )
				.attribute( "payload", Length.LONG32, false )
				.attribute( "retries", null, false )
				.attribute( "processAfter", null, false )
				.enumAttribute( "status", null, false );
		if ( tenantIdRequired ) {
			builder.tenantId( "tenantId" );
		}
		return builder.build();
	}
}
