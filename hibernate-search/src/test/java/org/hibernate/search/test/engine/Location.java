package org.hibernate.search.test.engine;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author: Gustavo Fernandes
 */
@Entity
@Indexed
public class Location {

	@Id
	@DocumentId
	@NumericField
	private int id;

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

	public Location(int id, Long counter, double latitude, Double longitude, Integer ranking, String description, Double multiple) {
		this.id = id;
		this.counter = counter;
		this.longitude = longitude;
		this.latitude = latitude;
		this.ranking = ranking;
		this.description = description;
		this.multiple = multiple;
	}

}
