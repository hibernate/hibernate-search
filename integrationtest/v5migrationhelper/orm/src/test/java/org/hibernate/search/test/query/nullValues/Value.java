/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.nullValues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Table(name = "\"value\"")
@Indexed
public class Value {
	@Id
	@Field(store = Store.YES)
	@GeneratedValue
	private int id;

	@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "_custom_token_")
	@Column(name = "\"value\"")
	private String value;

	@Field(analyze = Analyze.NO, store = Store.YES, indexNullAs = "fubar")
	private String fallback;

	public Value() {
	}

	public Value(String value) {
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFallback() {
		return fallback;
	}

	public void setFallback(String fallback) {
		this.fallback = fallback;
	}

}
