/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.outboxpolling.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// See AuthorService#triggerValidationFailure
@Entity(name = "Author")
@Indexed
public class AuthorWithSlightlyDifferentIndex {

	@Id
	@GeneratedValue
	private Integer id;

	@FullTextField(analyzer = AnalyzerNames.DEFAULT)
	private String name;

	public AuthorWithSlightlyDifferentIndex() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
