/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.dependencies.containers.fromotherentity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;

//tag::include[]
@Entity
@Indexed
@TypeBinding(binder = @TypeBinderRef(type = ScientificPapersReferencedByBinder.class)) // <1>
public class ScientificPaper {

	@Id
	private Integer id;

	private String title;

	@ManyToMany
	private List<ScientificPaper> references = new ArrayList<>();

	public ScientificPaper() {
	}

	// Getters and setters
	// ...

	//tag::getters-setters[]
	public ScientificPaper(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ScientificPaper> getReferences() {
		return references;
	}
	//end::getters-setters[]
}
//end::include[]
