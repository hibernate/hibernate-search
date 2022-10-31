/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
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
	 */
	default StandalonePojoMappingConfigurationContext addEntityType(Class<?> type) {
		addEntityType( type, type.getSimpleName() );
		return this;
	}

	/**
	 * Register a type as an entity type with the given name.
	 *
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param entityName The name of the entity.
	 */
	default StandalonePojoMappingConfigurationContext addEntityType(Class<?> type, String entityName) {
		addEntityType( type, entityName, null );
		return this;
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 */
	default StandalonePojoMappingConfigurationContext addEntityTypes(Class<?>... types) {
		for ( Class<?> type : types ) {
			addEntityType( type );
		}
		return this;
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 */
	default StandalonePojoMappingConfigurationContext addEntityTypes(Iterable<Class<?>> types) {
		for ( Class<?> type : types ) {
			addEntityType( type );
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
	 */
	default <E> StandalonePojoMappingConfigurationContext addEntityType(Class<E> type, EntityConfigurer<E> configurer) {
		addEntityType( type, type.getSimpleName(), configurer );
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
	 */
	<E> StandalonePojoMappingConfigurationContext addEntityType(Class<E> type, String entityName, EntityConfigurer<E> configurer);

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
