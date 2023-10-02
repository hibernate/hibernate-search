/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.query;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
@Cacheable
public class Book {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english", projectable = Projectable.YES)
	@KeywordField(name = "title_sort", normalizer = "english", sortable = Sortable.YES)
	private String title;

	public Book() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
