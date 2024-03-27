/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.lucene.type.asnative;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;

// tag::include[]
@Entity
@Indexed
public class WebPage {

	@Id
	private Integer id;

	@NonStandardField( // <1>
			valueBinder = @ValueBinderRef(type = PageRankValueBinder.class) // <2>
	)
	private Float pageRank;

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Float getPageRank() {
		return pageRank;
	}

	public void setPageRank(Float pageRank) {
		this.pageRank = pageRank;
	}
	// end::getters-setters[]
}
// end::include[]
