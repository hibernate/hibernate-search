/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * A context to map a property to the index schema.
 */
public interface PropertyMappingContext {

	/**
	 * Stops mapping the current property and switches to another one on the same parent type.
	 * @param propertyName The name of another property <strong>on the same type</strong> as the current property
	 * (not a nested property).
	 * @return A mapping context for that property.
	 */
	PropertyMappingContext property(String propertyName);

	/**
	 * Maps the property to the identifier of documents in the index.
	 * <p>
	 * This is only taken into account on {@link Indexed} types.
	 * @return A context to configure the ID mapping or to add more mappings to the same property.
	 * @see DocumentId
	 */
	PropertyDocumentIdMappingContext documentId();

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see PropertyBridge
	 */
	PropertyMappingContext bridge(Class<? extends PropertyBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see PropertyBridge
	 */
	PropertyMappingContext bridge(BeanReference<? extends PropertyBridge> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 * @see PropertyBridge
	 */
	PropertyMappingContext bridge(BridgeBuilder<? extends PropertyBridge> builder);

	/**
	 * @param builder A marker builder.
	 * @return {@code this}, for method chaining.
	 * @see MarkerBuilder
	 * @see org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker.Builder
	 * @see org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker.Builder
	 */
	PropertyMappingContext marker(MarkerBuilder builder);

	/**
	 * Maps the property to a field in the index with the same name as this property.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see GenericField
	 */
	PropertyGenericFieldMappingContext genericField();

	/**
	 * Maps the property to a field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see GenericField
	 * @see GenericField#name()
	 */
	PropertyGenericFieldMappingContext genericField(String relativeFieldName);

	/**
	 * Maps the property to a full-text field in the index with the same name as this property.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see FullTextField
	 */
	PropertyFullTextFieldMappingContext fullTextField();

	/**
	 * Maps the property to a full-text field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see FullTextField
	 * @see FullTextField#name()
	 */
	PropertyFullTextFieldMappingContext fullTextField(String relativeFieldName);

	/**
	 * Maps the property to a keyword field in the index with the same name as this property.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see KeywordField
	 */
	PropertyKeywordFieldMappingContext keywordField();

	/**
	 * Maps the property to a keyword field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see KeywordField
	 * @see KeywordField#name()
	 */
	PropertyKeywordFieldMappingContext keywordField(String relativeFieldName);

	/**
	 * Maps the property to a scaled number field in the index with the same name as this property.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see ScaledNumberField
	 */
	PropertyScaledNumberFieldMappingContext scaledNumberField();

	/**
	 * Maps the property to a scaled number field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A context to configure the field or to add more mappings to the same property.
	 * @see ScaledNumberField
	 * @see ScaledNumberField#name()
	 */
	PropertyScaledNumberFieldMappingContext scaledNumberField(String relativeFieldName);

	/**
	 * Maps the property to an object field whose fields are the same as those defined in the property type.
	 * @return A context to configure the embedding or to add more mappings to the same property.
	 * @see IndexedEmbedded
	 */
	PropertyIndexedEmbeddedMappingContext indexedEmbedded();

	/**
	 * Assuming the property represents an association on a entity type A to entity type B,
	 * defines the inverse side of an association, i.e. the path from B to A.
	 * <p>
	 * This is generally not needed, as inverse sides of associations should generally be inferred by the mapper.
	 * For example, Hibernate ORM defines inverse sides using {@code @OneToMany#mappedBy}, {@code @OneToOne#mappedBy}, etc.,
	 * and the Hibernate ORM mapper will register these inverse sides automatically.
	 *
	 * @param inversePath The path representing the inverse side of the association.
	 * @return A context to configure the association inverse side in more details
	 * or to add more mappings to the same property.
	 * @see AssociationInverseSide
	 * @see PojoModelPath#ofValue(String)
	 * @see PojoModelPath#ofValue(String, ContainerExtractorPath)
	 * @see PojoModelPath#builder()
	 */
	AssociationInverseSideMappingContext associationInverseSide(PojoModelPathValueNode inversePath);

	/**
	 * Defines how a dependency of the indexing process to this property
	 * should affect automatic reindexing.
	 * @return A context to configure indexing dependency
	 * or to add more mappings to the same property.
	 */
	IndexingDependencyMappingContext indexingDependency();

}
