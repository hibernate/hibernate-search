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
package org.hibernate.search.test.proxy;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Proxy;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "Book")
@Proxy(proxyClass = IBook.class)
public class Book implements IBook {

	private Integer id;
	private String body;
	private String summary;
	private Set<IAuthor> authors = new HashSet<IAuthor>();

	public Book() {
	}

	public Book(Integer id, String summary, String body) {
		this.id = id;
		this.summary = summary;
		this.body = body;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(targetEntity = Author.class, mappedBy = "book", cascade = CascadeType.ALL)
	@IndexedEmbedded(targetElement = Author.class)
	public Set<IAuthor> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<IAuthor> authors) {
		this.authors = authors;
	}

	@Field
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Fields({
			@Field(store = Store.YES),
			@Field(name = "summary_forSort", analyze = Analyze.NO, store = Store.YES)
	})
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
}
