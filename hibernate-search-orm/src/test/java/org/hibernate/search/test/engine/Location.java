package org.hibernate.search.test.engine;

import org.hibernate.search.annotations.CacheFromIndex;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;

import static org.hibernate.search.annotations.FieldCacheType.CLASS;
import static org.hibernate.search.annotations.FieldCacheType.ID;

/**
 * @author: Gustavo Fernandes
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Entity
@Indexed (index = "numeric_field_test")
@CacheFromIndex({CLASS,ID})
public class Location {

	@Id
	@DocumentId(name="overridenFieldName")
	@NumericField
	private int id;

	public int getId() {
		return id;
	}

	@IndexedEmbedded
	private Country country;

	@Field(name = "myCounter") @NumericField(forField = "myCounter")
	private Long counter;

	@Field(store = Store.YES) @NumericField(forField = "latitude", precisionStep = 1)
	private double latitude;

	@Field(store = Store.YES) @NumericField(forField = "longitude")
	private Double longitude;

	@Field @NumericField
	private Integer ranking;

	@Field
	private String description;

	@OneToMany(mappedBy = "location", cascade = { CascadeType.ALL}) @IndexedEmbedded
	private Collection<PinPoint> pinPoints = new ArrayList<PinPoint>();

	@Fields({
		@Field(name="strMultiple"),
		@Field
	})
	@NumericFields({
		@NumericField(forField = "strMultiple")
	})
	private Double multiple;

	public Location() {
	}

	public Location(int id, Long counter, double latitude, Double longitude,
					Integer ranking, String description, Double multiple, Country country) {
		this.id = id;
		this.counter = counter;
		this.longitude = longitude;
		this.latitude = latitude;
		this.ranking = ranking;
		this.description = description;
		this.multiple = multiple;
		this.country = country;
	}

	public void addPinPoints(PinPoint...pinPoints) {
		for (int i=0; i< pinPoints.length; i++) {
			pinPoints[i].setLocation(this);
			this.pinPoints.add(pinPoints[i]);
		}
	}

	public String getDescription() {
		return description;
	}
	
}
