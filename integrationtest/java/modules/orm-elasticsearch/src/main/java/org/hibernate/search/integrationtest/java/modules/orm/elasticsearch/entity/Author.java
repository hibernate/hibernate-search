/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.config.MyElasticsearchAnalysisConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Author {

	@Id
	@GeneratedValue
	private Integer id;

	@FullTextField(analyzer = MyElasticsearchAnalysisConfigurer.MY_ANALYZER)
	private String name;

	public Author() {
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
