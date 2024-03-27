/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.bridge;

import java.util.Date;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
@Table(name = "ABT_Entity")
public class ArrayBridgeTestEntity {

	static final String NULL_LANGUAGE_TOKEN = "PIRATE";
	static final String NULL_NUMERIC_TOKEN = "-555";
	static final int NULL_NUMERIC_TOKEN_INT = -555;

	private Long id;
	private String name;
	private Language[] nullIndexed = new Language[0];
	private String[] nullNotIndexed = new String[0];
	private Integer[] numericNullIndexed = new Integer[0];
	private Long[] numericNullNotIndexed = new Long[0];
	private float[] primitive = new float[0];

	private Date[] dates = new Date[0];

	public enum Language {
		ITALIAN, ENGLISH, PIRATE, KLINGON
	}

	@Id
	@GeneratedValue
	@Column(name = "array_id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "name")
	@Field(store = Store.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Field(indexNullAs = NULL_LANGUAGE_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "NullIndexed", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "nullIndexed")
	public Language[] getNullIndexed() {
		return nullIndexed;
	}

	public void setNullIndexed(Language[] nullIndexed) {
		this.nullIndexed = nullIndexed;
	}

	@Field(store = Store.YES, indexNullAs = NULL_NUMERIC_TOKEN, analyze = Analyze.NO)
	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "NumericNullIndexed", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "numericNullIndexed")
	public Integer[] getNumericNullIndexed() {
		return numericNullIndexed;
	}

	public void setNumericNullIndexed(Integer[] phoneNumbers) {
		this.numericNullIndexed = phoneNumbers;
	}

	@Field(store = Store.YES)
	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "NullNotIndexed", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "nullNotIndexed")
	public String[] getNullNotIndexed() {
		return nullNotIndexed;
	}

	public void setNullNotIndexed(String[] skipNullCollection) {
		this.nullNotIndexed = skipNullCollection;
	}

	@Field(store = Store.YES)
	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "NumericNullNotIndexed", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "numericNullNotIndexed")
	public Long[] getNumericNullNotIndexed() {
		return numericNullNotIndexed;
	}

	public void setNumericNullNotIndexed(Long[] numericSkipNullCollection) {
		this.numericNullNotIndexed = numericSkipNullCollection;
	}

	@Field(store = Store.YES)
	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "primitive", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "primitive")
	public float[] getPrimitive() {
		return primitive;
	}

	public void setPrimitive(float[] primitive) {
		this.primitive = primitive;
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	@ElementCollection
	@DateBridge(resolution = Resolution.SECOND)
	@OrderColumn
	@CollectionTable(name = "Dates", joinColumns = @JoinColumn(name = "array_id"))
	@Column(name = "dates")
	public Date[] getDates() {
		return dates;
	}

	public void setDates(Date[] dates) {
		this.dates = dates;
	}

	@Override
	public String toString() {
		return ArrayBridgeTestEntity.class.getSimpleName() + "[id=" + id + ", name=" + name + "]";
	}

}
