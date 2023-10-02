/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.shallow.correct;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

//tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	private String title;

	@ManyToOne
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW) // <1>
	private BookCategory category;

	public Book() {
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BookCategory getCategory() {
		return category;
	}

	public void setCategory(BookCategory category) {
		this.category = category;
	}
	//end::getters-setters[]
}
//end::include[]
