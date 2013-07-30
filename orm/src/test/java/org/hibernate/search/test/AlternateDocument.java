/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * Example of 2 entities mapped in the same index
 *
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "Documents")
public class AlternateDocument {
	private Long id;
	private String title;
	private String summary;
	private String text;

	public AlternateDocument() {
	}

	public AlternateDocument(Long id, String title, String summary, String text) {
		super();
		this.id = id;
		this.summary = summary;
		this.text = text;
		this.title = title;
	}

	@Id
	@DocumentId()
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(name = "alt_title", store = Store.YES)
	@Boost(2)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Field(name = "Abstract", store = Store.NO)
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Field(store = Store.NO)
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
