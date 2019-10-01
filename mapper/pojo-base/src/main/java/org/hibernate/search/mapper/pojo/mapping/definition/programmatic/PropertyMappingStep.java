/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
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
 * The step in a mapping definition where a property can be mapped.
 */
public interface PropertyMappingStep {

	/**
	 * Stops mapping the current property and switches to another one on the same parent type.
	 * @param propertyName The name of another property <strong>on the same type</strong> as the current property
	 * (not a nested property).
	 * @return A DSL step where the property mapping can be defined in more details.
	 */
	PropertyMappingStep property(String propertyName);

	/**
	 * Maps the property to the identifier of documents in the index.
	 * <p>
	 * This is only taken into account on {@link Indexed} types.
	 * @return A DSL step where the document ID mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see DocumentId
	 */
	PropertyMappingDocumentIdOptionsStep documentId();

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see PropertyBridge
	 */
	PropertyMappingStep bridge(Class<? extends PropertyBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see PropertyBridge
	 */
	PropertyMappingStep bridge(BeanReference<? extends PropertyBridge> bridgeReference);

	/**
	 * @param binder A {@link PropertyBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see PropertyBinder
	 */
	PropertyMappingStep binder(PropertyBinder<?> binder);

	/**
	 * @param binder A {@link MarkerBinder} responsible for creating a marker object.
	 * @return {@code this}, for method chaining.
	 * @see MarkerBinder
	 * @see GeoPointBinder#latitude()
	 * @see GeoPointBinder#longitude()
	 */
	PropertyMappingStep marker(MarkerBinder<?> binder);

	/**
	 * Maps the property to a field in the index with the same name as this property.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see GenericField
	 */
	PropertyMappingGenericFieldOptionsStep genericField();

	/**
	 * Maps the property to a field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see GenericField
	 * @see GenericField#name()
	 */
	PropertyMappingGenericFieldOptionsStep genericField(String relativeFieldName);

	/**
	 * Maps the property to a full-text field in the index with the same name as this property.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see FullTextField
	 */
	PropertyMappingFullTextFieldOptionsStep fullTextField();

	/**
	 * Maps the property to a full-text field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see FullTextField
	 * @see FullTextField#name()
	 */
	PropertyMappingFullTextFieldOptionsStep fullTextField(String relativeFieldName);

	/**
	 * Maps the property to a keyword field in the index with the same name as this property.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see KeywordField
	 */
	PropertyMappingKeywordFieldOptionsStep keywordField();

	/**
	 * Maps the property to a keyword field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see KeywordField
	 * @see KeywordField#name()
	 */
	PropertyMappingKeywordFieldOptionsStep keywordField(String relativeFieldName);

	/**
	 * Maps the property to a scaled number field in the index with the same name as this property.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see ScaledNumberField
	 */
	PropertyMappingScaledNumberFieldOptionsStep scaledNumberField();

	/**
	 * Maps the property to a scaled number field in the index with a custom name.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see ScaledNumberField
	 * @see ScaledNumberField#name()
	 */
	PropertyMappingScaledNumberFieldOptionsStep scaledNumberField(String relativeFieldName);

	/**
	 * Maps the property to an object field whose fields are the same as those defined in the property type.
	 * @return A DSL step where the indexed-embedded mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see IndexedEmbedded
	 */
	PropertyMappingIndexedEmbeddedStep indexedEmbedded();

	/**
	 * Assuming the property represents an association on a entity type A to entity type B,
	 * defines the inverse side of an association, i.e. the path from B to A.
	 * <p>
	 * This is generally not needed, as inverse sides of associations should generally be inferred by the mapper.
	 * For example, Hibernate ORM defines inverse sides using {@code @OneToMany#mappedBy}, {@code @OneToOne#mappedBy}, etc.,
	 * and the Hibernate ORM mapper will register these inverse sides automatically.
	 *
	 * @param inversePath The path representing the inverse side of the association.
	 * @return A DSL step where the association's inverse side can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see AssociationInverseSide
	 * @see PojoModelPath#ofValue(String)
	 * @see PojoModelPath#ofValue(String, ContainerExtractorPath)
	 * @see PojoModelPath#builder()
	 */
	AssociationInverseSideOptionsStep associationInverseSide(PojoModelPathValueNode inversePath);

	/**
	 * Defines how a dependency of the indexing process to this property
	 * should affect automatic reindexing.
	 * @return A DSL step where indexing dependency can be defined in more details,
	 * or where other elements can be mapped to the property.
	 */
	IndexingDependencyOptionsStep indexingDependency();

}
