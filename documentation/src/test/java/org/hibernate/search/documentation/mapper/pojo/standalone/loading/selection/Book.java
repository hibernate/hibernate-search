/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.selection;

import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

// tag::include[]
@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = MyLoadingBinder.class)) // <1>
@Indexed
public class Book {
	// end::include[]

	@DocumentId
	private String id;

	@FullTextField
	private String title;

	public Book(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
