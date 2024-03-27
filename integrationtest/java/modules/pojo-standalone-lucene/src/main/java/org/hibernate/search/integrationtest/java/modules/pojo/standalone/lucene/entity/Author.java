/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.entity;

import org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.config.MyLuceneAnalysisConfigurer;
import org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.service.SimulatedDatastore;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = SimulatedDatastore.AuthorLoadingBinder.class))
@Indexed
public class Author {

	@DocumentId
	private Integer id;

	@FullTextField(analyzer = MyLuceneAnalysisConfigurer.MY_ANALYZER)
	private String name;

	public Author() {
	}

	public Author(Integer id, String name) {
		this.id = id;
		this.name = name;
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
