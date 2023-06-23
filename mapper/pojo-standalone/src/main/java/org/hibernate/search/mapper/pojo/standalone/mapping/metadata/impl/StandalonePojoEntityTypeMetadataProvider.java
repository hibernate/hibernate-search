/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class StandalonePojoEntityTypeMetadataProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<PojoRawTypeModel<?>, StandalonePojoEntityTypeMetadata<?>> entityTypeMetadata;

	private StandalonePojoEntityTypeMetadataProvider(
			Map<PojoRawTypeModel<?>, StandalonePojoEntityTypeMetadata<?>> entityTypeMetadata) {
		this.entityTypeMetadata = entityTypeMetadata;
	}

	public Set<PojoRawTypeModel<?>> allEntityTypes() {
		return entityTypeMetadata.keySet();
	}

	@SuppressWarnings("unchecked")
	public <E> StandalonePojoEntityTypeMetadata<E> get(PojoRawTypeModel<E> type) {
		return (StandalonePojoEntityTypeMetadata<E>) entityTypeMetadata.get( type );
	}

	public static class Builder {

		private final StandalonePojoBootstrapIntrospector introspector;

		// Use a LinkedHashMap for deterministic iteration
		private final Map<PojoRawTypeModel<?>, EntityDefinition<?>> entityDefinitionByType = new LinkedHashMap<>();
		private final Map<String, PojoRawTypeModel<?>> entityTypeByName = new LinkedHashMap<>();

		public Builder(StandalonePojoBootstrapIntrospector introspector) {
			this.introspector = introspector;
		}

		public <E> void addEntityType(Class<E> clazz, String entityName, EntityConfigurer<E> configurerOrNull) {
			PojoRawTypeModel<E> type = introspector.typeModel( clazz );
			entityDefinitionByType.merge( type, new EntityDefinition<>( type, entityName, configurerOrNull ),
					EntityDefinition::mergeWith );
			PojoRawTypeModel<?> previousType = entityTypeByName.putIfAbsent( entityName, type );
			if ( previousType != null && !previousType.equals( type ) ) {
				throw log.multipleEntityTypesWithSameName( entityName, previousType, type );
			}
		}

		public StandalonePojoEntityTypeMetadataProvider build() {
			TypeMetadataContributorProvider<EntityConfigurer<?>> contributorProvider =
					buildMetadataContributorProvider();
			// Use a LinkedHashMap for deterministic iteration
			Map<PojoRawTypeModel<?>, StandalonePojoEntityTypeMetadata<?>> entityTypeMetadata = new LinkedHashMap<>();
			for ( Map.Entry<PojoRawTypeModel<?>, EntityDefinition<?>> entry : entityDefinitionByType.entrySet() ) {
				entityTypeMetadata.put(
						entry.getKey(),
						buildEntityTypeMetadata( entry.getValue(), contributorProvider )
				);
			}
			return new StandalonePojoEntityTypeMetadataProvider( entityTypeMetadata );
		}

		TypeMetadataContributorProvider<EntityConfigurer<?>> buildMetadataContributorProvider() {
			TypeMetadataContributorProvider.Builder<EntityConfigurer<?>> builder =
					TypeMetadataContributorProvider.builder();
			for ( Map.Entry<PojoRawTypeModel<?>, EntityDefinition<?>> entry : entityDefinitionByType.entrySet() ) {
				EntityConfigurer<?> configurerOrNull = entry.getValue().configurerOrNull;
				if ( configurerOrNull != null ) {
					builder.contributor( entry.getKey(), configurerOrNull );
				}
			}
			return builder.build();
		}

		private <E> StandalonePojoEntityTypeMetadata<E> buildEntityTypeMetadata(EntityDefinition<E> definition,
				TypeMetadataContributorProvider<EntityConfigurer<?>> contributorProvider) {
			StandalonePojoEntityTypeMetadata.Builder<E> builder =
					new StandalonePojoEntityTypeMetadata.Builder<>( definition.entityName );
			for ( EntityConfigurer<?> configurer : contributorProvider.get( definition.type ) ) {
				@SuppressWarnings("unchecked") // By constructions, all configurers returned here apply to supertypes of E
				EntityConfigurer<? super E> castConfigurer = (EntityConfigurer<? super E>) configurer;
				castConfigurer.configure( toConfigurationContext( builder ) );
			}
			return builder.build();
		}

		private static <E, E2 extends E> EntityConfigurationContext<E> toConfigurationContext(
				StandalonePojoEntityTypeMetadata.Builder<E2> builder) {
			return new EntityConfigurationContext<E>() {
				@Override
				public void selectionLoadingStrategy(SelectionLoadingStrategy<? super E> strategy) {
					builder.selectionLoadingStrategy( strategy );
				}

				@Override
				public void massLoadingStrategy(MassLoadingStrategy<? super E, ?> strategy) {
					builder.massLoadingStrategy( strategy );
				}
			};
		}
	}

	private static class EntityDefinition<E> {
		private final PojoRawTypeModel<E> type;
		private final String entityName;
		private final EntityConfigurer<E> configurerOrNull;

		private EntityDefinition(PojoRawTypeModel<E> type,
				String entityName, EntityConfigurer<E> configurerOrNull) {
			this.type = type;
			this.entityName = entityName;
			this.configurerOrNull = configurerOrNull;
		}

		public EntityDefinition<E> mergeWith(EntityDefinition<?> unknownTypeOther) {
			if ( !type.equals( unknownTypeOther.type ) ) {
				throw log.multipleEntityTypeDefinitions( type );
			}
			@SuppressWarnings("unchecked")
			EntityDefinition<E> other = (EntityDefinition<E>) unknownTypeOther;
			if ( !entityName.equals( other.entityName ) ) {
				throw log.multipleEntityTypeDefinitions( type );
			}
			EntityConfigurer<E> configurerOrNull;
			if ( this.configurerOrNull == null ) {
				configurerOrNull = other.configurerOrNull;
			}
			else if ( other.configurerOrNull == null ) {
				configurerOrNull = this.configurerOrNull;
			}
			else {
				throw log.multipleEntityTypeDefinitions( type );
			}
			return new EntityDefinition<>( type, entityName, configurerOrNull );
		}
	}
}
