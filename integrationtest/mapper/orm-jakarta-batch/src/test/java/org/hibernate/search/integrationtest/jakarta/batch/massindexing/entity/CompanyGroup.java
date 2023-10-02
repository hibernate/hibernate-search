/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

/**
 * @author Yoann Rodiere
 */
@Entity
@Indexed
public class CompanyGroup {

	@Id
	private String id;

	@ManyToMany
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private List<Company> companies = new ArrayList<>();

	public CompanyGroup() {
	}

	public CompanyGroup(String id, Company... companies) {
		this.id = id;
		Collections.addAll( this.companies, companies );
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Company> getCompanies() {
		return companies;
	}

	public void setCompanies(List<Company> companies) {
		this.companies = companies;
	}
}
