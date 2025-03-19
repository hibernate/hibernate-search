/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;

@Indexed
@GeoPointBinding(fieldName = "placeOfBirth", markerSet = "birth")
@GeoPointBinding(fieldName = "placeOfDeath", markerSet = "death")
public class MyIndexedEntity {

	@DocumentId
	private String id;

	@GenericField
	private LocalDate date;

	@KeywordField(projectable = Projectable.YES)
	private String keyword;

	@FullTextField
	private String text;

	//@PropertyBinding(binder = @PropertyBinderRef(type = ISBNBinder.class))
	private ISBN isbn;

	@PropertyBinding(binder = @PropertyBinderRef(type = SomeRandomTypeBinder.class))
	private SomeRandomType someRandomType;

	@IndexedEmbedded
	private MyEmbeddedEntity embedded;

	@KeywordField
	private Set<String> keywords;

	@FullTextField
	private SomeGenerics.MyStringList fullTextFields;

	@FullTextField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY))
	private SomeGenerics.MyStringStringMap mapKeys;

	@FullTextField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
	private SomeGenerics.MyStringStringMap mapValues;

	@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
	private SomeGenerics.MyStringKeyMap<LocalDate> dateValues;

	@IndexedEmbedded
	private List<MyEmbeddedEntity> embeddedList;

	@VectorField(dimension = 15)
	private byte[] bytes;

	@VectorField(dimension = 5)
	private float[] floats;

	@Latitude(markerSet = "birth")
	private Double placeOfBirthLatitude;

	@Longitude(markerSet = "birth")
	private Double placeOfBirthLongitude;

	@Latitude(markerSet = "death")
	private Double placeOfDeathLatitude;

	@Longitude(markerSet = "death")
	private Double placeOfDeathLongitude;

	@KeywordField
	private MyEnum myEnum;

	@GenericField
	private GeoPoint point;

	private String someString;

}
