/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.session;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Email {
	@Id
	@DocumentId
	private Long id;

	@Field
	private String title;

	@Field
	private String body;

	private String header;

	@IndexedEmbedded
	@ManyToOne(fetch = FetchType.LAZY)
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private Domain domain;

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}
}
