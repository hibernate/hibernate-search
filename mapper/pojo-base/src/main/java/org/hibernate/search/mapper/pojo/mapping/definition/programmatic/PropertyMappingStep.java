/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a mapping definition where a property can be mapped.
 */
public interface PropertyMappingStep {

	/**
	 * @return A DSL step where the mapping can be defined
	 * for the type hosting this property.
	 */
	TypeMappingStep hostingType();

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
	 * Define a property binder, responsible for creating a bridge.
	 * To pass some parameters to the bridge,
	 * use the method {@link #binder(PropertyBinder, Map)} instead.
	 *
	 * @param binder A {@link PropertyBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see PropertyBinder
	 */
	default PropertyMappingStep binder(PropertyBinder binder) {
		return binder( binder, Collections.emptyMap() );
	}

	/**
	 * Define a property binder, responsible for creating a bridge.
	 * With this method it is possible to pass a set of parameters to the binder.
	 *
	 * @param binder A {@link PropertyBinder} responsible for creating a bridge.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 */
	PropertyMappingStep binder(PropertyBinder binder, Map<String, Object> params);

	/**
	 * Define a marker binder, responsible for creating a marker object.
	 * To use some parameters to create the marker object,
	 * use the method {@link #marker(MarkerBinder, Map)} instead.
	 *
	 * @param binder A {@link MarkerBinder} responsible for creating a marker object.
	 * @return {@code this}, for method chaining.
	 * @see MarkerBinder
	 * @see GeoPointBinder#latitude()
	 * @see GeoPointBinder#longitude()
	 */
	default PropertyMappingStep marker(MarkerBinder binder) {
		return marker( binder, Collections.emptyMap() );
	}

	/**
	 * Define a marker binder, responsible for creating a marker object.
	 * With this method it is possible to pass a set of parameters to the binder,
	 * so that they can be used to create the marker object.
	 *
	 * @param binder A {@link MarkerBinder} responsible for creating a marker object.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see MarkerBinder
	 * @see GeoPointBinder#latitude()
	 * @see GeoPointBinder#longitude()
	 */
	PropertyMappingStep marker(MarkerBinder binder, Map<String, Object> params);

	/**
	 * Maps the property to a field of standard type in the index with the same name as this property.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see GenericField
	 */
	PropertyMappingGenericFieldOptionsStep genericField();

	/**
	 * Maps the property to a field of standard type in the index with a custom name.
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
	 * Maps the property to a field of non-standard type in the index with the same name as this property.
	 * <p>
	 * This is for advanced use cases, when defining a field whose type is only supported in a specific backend.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see NonStandardField
	 */
	PropertyMappingFieldOptionsStep<?> nonStandardField();

	/**
	 * Maps the property to a field of non-standard type in the index with a custom name.
	 * <p>
	 * This is for advanced use cases, when defining a field whose type is only supported in a specific backend.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see NonStandardField
	 * @see NonStandardField#name()
	 */
	PropertyMappingFieldOptionsStep<?> nonStandardField(String relativeFieldName);

	/**
	 * Maps the property to an object field whose fields are the same as those defined in the property type,
	 * using the name of this property as the name of the object field.
	 * @return A DSL step where the indexed-embedded mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see IndexedEmbedded
	 */
	PropertyMappingIndexedEmbeddedStep indexedEmbedded();

	/**
	 * Maps the property to an object field whose fields are the same as those defined in the property type,
	 * using the given custom name as the name of the object field.
	 * @param relativeFieldName The name of the object field created for this indexed-embedded mapping.
	 * @return A DSL step where the indexed-embedded mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 * @see IndexedEmbedded
	 * @see IndexedEmbedded#name()
	 */
	PropertyMappingIndexedEmbeddedStep indexedEmbedded(String relativeFieldName);

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

	/**
	 * Maps the property to a vector field in the index with the same name as this property.
	 *
	 * @param dimension The number of dimensions (array length) of vectors to be indexed.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 *
	 * @see VectorField
	 * @see VectorField#dimension()
	 */
	@Incubating
	PropertyMappingVectorFieldOptionsStep vectorField(int dimension);

	/**
	 * Maps the property to a vector field in the index with a custom name.
	 *
	 * @param dimension The number of dimensions (array length) of vectors to be indexed.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 *
	 * @see VectorField
	 * @see VectorField#dimension()
	 * @see VectorField#name()
	 */
	@Incubating
	PropertyMappingVectorFieldOptionsStep vectorField(int dimension, String relativeFieldName);

	/**
	 * Maps the property to a vector field in the index with the same name as this property.
	 * <p>
	 * In this case the {@link VectorField#dimension() number of dimensions} (array length) of vectors to be indexed
	 * is expected to be provided through other means, e.g. via a {@link PropertyMappingVectorFieldOptionsStep#valueBinder(ValueBinder) binder}.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 *
	 * @see VectorField
	 * @see VectorField#dimension()
	 * @see VectorField#valueBinder()
	 */
	@Incubating
	PropertyMappingVectorFieldOptionsStep vectorField();

	/**
	 * Maps the property to a vector field in the index with a custom name.
	 * <p>
	 * In this case the {@link VectorField#dimension() number of dimensions} (array length) of vectors to be indexed
	 * is expected to be provided through other means, e.g. via a {@link PropertyMappingVectorFieldOptionsStep#valueBinder(ValueBinder) binder}.
	 * @param relativeFieldName The name of the index field.
	 * @return A DSL step where the field mapping can be defined in more details,
	 * or where other elements can be mapped to the property.
	 *
	 * @see VectorField
	 * @see VectorField#dimension()
	 * @see VectorField#name()
	 * @see VectorField#valueBinder()
	 */
	@Incubating
	PropertyMappingVectorFieldOptionsStep vectorField(String relativeFieldName);

}
