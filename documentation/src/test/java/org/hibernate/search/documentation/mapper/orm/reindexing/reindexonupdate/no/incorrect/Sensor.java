/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.no.incorrect;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

//tag::include[]
@Entity
@Indexed
public class Sensor {

	@Id
	private Integer id;

	@FullTextField
	private String name; // <1>

	@KeywordField
	private SensorStatus status; // <1>

	@Column(name = "\"value\"")
	private double value; // <2>

	@GenericField
	private double rollingAverage; // <3>

	public Sensor() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SensorStatus getStatus() {
		return status;
	}

	public void setStatus(SensorStatus status) {
		this.status = status;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getRollingAverage() {
		return rollingAverage;
	}

	public void setRollingAverage(double rollingAverage) {
		this.rollingAverage = rollingAverage;
	}
	//end::getters-setters[]
}
//end::include[]
