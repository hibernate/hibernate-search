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
public class MyIndexedGetterEntity {

	private String id;

	private LocalDate date;

	private String keyword;

	private String text;

	private ISBN isbn;

	private SomeRandomType someRandomType;

	private MyEmbeddedEntity embedded;

	private Set<String> keywords;

	private SomeGenerics.MyStringList fullTextFields;

	private SomeGenerics.MyStringStringMap mapKeys;

	private SomeGenerics.MyStringStringMap mapValues;

	private SomeGenerics.MyStringKeyMap<LocalDate> dateValues;

	private List<MyEmbeddedEntity> embeddedList;

	private byte[] bytes;

	private float[] floats;

	private Double placeOfBirthLatitude;

	private Double placeOfBirthLongitude;

	private Double placeOfDeathLatitude;

	private Double placeOfDeathLongitude;

	private MyEnum myEnum;

	@KeywordField
	private MyEnum myfieldEnum;

	private GeoPoint point;

	private String someString;

	@DocumentId
	public String getId() {
		return id;
	}

	@GenericField
	public LocalDate getDate() {
		return date;
	}

	@KeywordField(projectable = Projectable.YES)
	public String getKeyword() {
		return keyword;
	}

	@FullTextField
	public String getText() {
		return text;
	}

	// @PropertyBinding(binder = @PropertyBinderRef(type = ISBNBinder.class))
	public ISBN getIsbn() {
		return isbn;
	}

	@PropertyBinding(binder = @PropertyBinderRef(type = SomeRandomTypeBinder.class))
	public SomeRandomType getSomeRandomType() {
		return someRandomType;
	}

	@IndexedEmbedded
	public MyEmbeddedEntity getEmbedded() {
		return embedded;
	}

	@KeywordField
	public Set<String> getKeywords() {
		return keywords;
	}

	@FullTextField
	public SomeGenerics.MyStringList getFullTextFields() {
		return fullTextFields;
	}

	@FullTextField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY))
	public SomeGenerics.MyStringStringMap getMapKeys() {
		return mapKeys;
	}

	@FullTextField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
	public SomeGenerics.MyStringStringMap getMapValues() {
		return mapValues;
	}

	@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
	public SomeGenerics.MyStringKeyMap<LocalDate> getDateValues() {
		return dateValues;
	}

	@IndexedEmbedded
	public List<MyEmbeddedEntity> getEmbeddedList() {
		return embeddedList;
	}

	@VectorField(dimension = 15)
	public byte[] getBytes() {
		return bytes;
	}

	@VectorField(dimension = 5)
	public float[] getFloats() {
		return floats;
	}

	@Latitude(markerSet = "birth")
	public Double getPlaceOfBirthLatitude() {
		return placeOfBirthLatitude;
	}

	@Longitude(markerSet = "birth")
	public Double getPlaceOfBirthLongitude() {
		return placeOfBirthLongitude;
	}

	@Latitude(markerSet = "death")
	public Double getPlaceOfDeathLatitude() {
		return placeOfDeathLatitude;
	}

	@Longitude(markerSet = "death")
	public Double getPlaceOfDeathLongitude() {
		return placeOfDeathLongitude;
	}

	@KeywordField
	public MyEnum getMyEnum() {
		return myEnum;
	}

	@GenericField
	public GeoPoint getPoint() {
		return point;
	}

	public String getSomeString() {
		return someString;
	}
}
