/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.directfieldmapping;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	// tag::direct-field-mapping[]
	@FullTextField(analyzer = "english", projectable = Projectable.YES) // <1>
	@KeywordField(name = "title_sort", normalizer = "english", sortable = Sortable.YES) // <2>
	private String title;

	@GenericField(projectable = Projectable.YES, sortable = Sortable.YES) // <3>
	private Integer pageCount;
	// end::direct-field-mapping[]

	public Book() {
	}

	// Getters and setters
	// ...

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}
}
