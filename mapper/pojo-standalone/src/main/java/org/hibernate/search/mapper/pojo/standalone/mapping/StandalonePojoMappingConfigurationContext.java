/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.EntityConfigurerLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface StandalonePojoMappingConfigurationContext {

	/**
	 * Starts the definition of the programmatic mapping.
	 *
	 * @return A context to define the programmatic mapping.
	 */
	ProgrammaticMappingConfigurationContext programmaticMapping();

	/**
	 * Starts the definition of the annotation mapping.
	 *
	 * @return A context to define the annotation mapping.
	 */
	AnnotationMappingConfigurationContext annotationMapping();

	/**
	 * Starts the definition of container extractors available for use in mappings.
	 *
	 * @return A context to define container extractors.
	 */
	ContainerExtractorConfigurationContext containerExtractors();

	/**
	 * Starts the definition of bridges to apply by default in mappings.
	 *
	 * @return A context to define default bridges.
	 */
	BridgesConfigurationContext bridges();

	/**
	 * Register a type as an entity type with the default name, its simple class name.
	 *
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @deprecated Use {@link SearchEntity} or {@code programmaticMapping().type( type ).searchEntity()} instead.
	 */
	@Deprecated
	default StandalonePojoMappingConfigurationContext addEntityType(Class<?> type) {
		programmaticMapping().type( type ).searchEntity();
		return this;
	}

	/**
	 * Register a type as an entity type with the given name.
	 *
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param entityName The name of the entity.
	 * @deprecated Use {@link SearchEntity} or {@code programmaticMapping().type( type ).searchEntity().name( entityName )} instead.
	 */
	@Deprecated
	default StandalonePojoMappingConfigurationContext addEntityType(Class<?> type, String entityName) {
		programmaticMapping().type( type ).searchEntity().name( entityName );
		return this;
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @deprecated Use {@link SearchEntity} or {@code programmaticMapping().type( type ).searchEntity()} for each type instead.
	 */
	@Deprecated
	default StandalonePojoMappingConfigurationContext addEntityTypes(Class<?>... types) {
		for ( Class<?> type : types ) {
			programmaticMapping().type( type ).searchEntity();
		}
		return this;
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @deprecated Use {@link SearchEntity} or {@code programmaticMapping().type( type ).searchEntity()} for each type instead.
	 */
	@Deprecated
	default StandalonePojoMappingConfigurationContext addEntityTypes(Iterable<Class<?>> types) {
		for ( Class<?> type : types ) {
			programmaticMapping().type( type ).searchEntity();
		}
		return this;
	}

	/**
	 * Register a type as an entity type with the default name, its simple class name.
	 *
	 * @param <E> The entity type.
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param configurer The configurer, to provide additional information about the entity type.
	 * @deprecated Use {@link SearchEntity} or {@code programmaticMapping().type( type ).searchEntity().loadingBinder( binder )} instead.
	 */
	@Deprecated
	default <E> StandalonePojoMappingConfigurationContext addEntityType(Class<E> type,
			org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer<E> configurer) {
		programmaticMapping().type( type ).searchEntity()
				.loadingBinder( configurer == null ? null : new EntityConfigurerLoadingBinder<>( type, configurer ) );
		return this;
	}

	/**
	 * Register a type as an entity type with the given name.
	 *
	 * @param <E> The entity type.
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param entityName The name of the entity.
	 * @param configurer The configurer, to provide additional information about the entity type.
	 * @deprecated Use {@link SearchEntity} or {@code programmaticMapping().type( type ).searchEntity().name( entityName ).loadingBinder( binder )} instead.
	 */
	@Deprecated
	default <E> StandalonePojoMappingConfigurationContext addEntityType(Class<E> type, String entityName,
			org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer<E> configurer) {
		programmaticMapping().type( type ).searchEntity().name( entityName )
				.loadingBinder( configurer == null ? null : new EntityConfigurerLoadingBinder<>( type, configurer ) );
		return this;
	}

	/**
	 * Defines the default depth of automatic reindexing.
	 * <p>
	 * Keep the default value ({@link ReindexOnUpdate#DEFAULT} if your entity model is a graph (normalized model, e.g. in ORMs);
	 * pass {@link ReindexOnUpdate#SHALLOW} if your entity model is a collection of trees (denormalized model, e.g. in a document datastore).
	 * <p>
	 * The exact behavior is as follows:
	 * <ul>
	 *     <li>If set to {@link ReindexOnUpdate#DEFAULT}, when entity A is {@link IndexedEmbedded indexed-embedded} in entity B,
	 *     and a relevant property of entity A changes, then Hibernate Search will trigger reindexing of
	 *     entity A (if appropriate) <em>and</em> entity B (if appropriate).</li>
	 *     <li>If set to {@link ReindexOnUpdate#SHALLOW}, when entity A is {@link IndexedEmbedded indexed-embedded} in entity B,
	 *     and a relevant property of entity A changes, then Hibernate Search will trigger reindexing of
	 *     entity A (if appropriate) <em>only</em>, and not entity B.</li>
	 *     <li>If set to {@link ReindexOnUpdate#NO}, when entity A is {@link IndexedEmbedded indexed-embedded} in entity B,
	 *     and a relevant property of entity A changes, then Hibernate Search will trigger reindexing of
	 *     neither entity A nor entity B. The only way to trigger reindexing will be to force it,
	 *     e.g. with {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}
	 *     (without passing a list of dirty paths)
	 *     or {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object, DocumentRoutesDescriptor, Object, boolean, boolean, String...)}
	 *     (with {@code forceSelfDirty = true} or {@code forceContainedDirty = true}) </li>
	 * </ul>
	 *
	 * @param defaultReindexOnUpdate The default behavior
	 */
	void defaultReindexOnUpdate(ReindexOnUpdate defaultReindexOnUpdate);

	/**
	 * @param providedIdentifierBridge An identifier bridge to use by default for entities that don't have a property annotated
	 * with {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId}.
	 * Caution: the bridge will be applied to the whole entity, with the expectation that the identifier never changes for a given entity.
	 */
	void providedIdentifierBridge(BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge);
}
