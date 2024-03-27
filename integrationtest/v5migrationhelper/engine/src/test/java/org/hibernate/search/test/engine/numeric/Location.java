/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.numeric;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Gustavo Fernandes
 */
@Indexed(index = "numeric_field_test")
class Location {

	@DocumentId
	@Field(name = "overriddenFieldName")
	@NumericField(forField = "overriddenFieldName")
	private int id;

	public int getId() {
		return id;
	}

	@IndexedEmbedded
	private Country country;

	@Field(name = "myCounter")
	private Long counter;

	@Field(store = Store.YES)
	private double latitude;

	@Field(store = Store.YES)
	private Double longitude;

	@Field
	private Integer ranking;

	@Field
	private String description;

	@IndexedEmbedded
	private final Collection<PinPoint> pinPoints = new ArrayList<PinPoint>();

	@Fields({
			@Field(name = "strMultiple"),
			@Field
	})
	@NumericFields({
			@NumericField(forField = "strMultiple")
	})
	private Double multiple;

	@Field(store = Store.YES)
	@NumericField
	private short importance;

	@Field(store = Store.YES)
	@NumericField
	private Short fallbackImportance;

	@Field(store = Store.YES)
	@NumericField
	private byte popularity;

	@Field(store = Store.YES)
	@NumericField
	private Byte fallbackPopularity;


	public Location() {
	}

	public Location(int id, Long counter, double latitude, Double longitude,
			Integer ranking, String description, Double multiple, Country country, short importance, byte popularity) {
		this.id = id;
		this.counter = counter;
		this.longitude = longitude;
		this.latitude = latitude;
		this.ranking = ranking;
		this.description = description;
		this.multiple = multiple;
		this.country = country;
		this.importance = importance;
		this.fallbackImportance = importance;
		this.popularity = popularity;
		this.fallbackPopularity = popularity;
	}

	public void addPinPoints(PinPoint... pinPoints) {
		for ( int i = 0; i < pinPoints.length; i++ ) {
			pinPoints[i].setLocation( this );
			this.pinPoints.add( pinPoints[i] );
		}
	}

	public String getDescription() {
		return description;
	}

}

