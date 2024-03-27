/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "container")
@DiscriminatorColumn(name = "containerType", discriminatorType = DiscriminatorType.STRING)
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "container")
@Indexed
public class Container extends AbstractEntity {

	@Id()
	private Long containerId;

	@Column(length = 255)
	@FullTextField
	private String color;

	/**
	 * @return the containerId
	 */
	public Long getContainerId() {
		return containerId;
	}

	/**
	 * @param containerId the containerId to set
	 */
	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	public String getColor() {
		return color;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(String color) {
		this.color = color;
	}
}
