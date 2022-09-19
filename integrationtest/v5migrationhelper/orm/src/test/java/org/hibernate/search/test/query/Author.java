/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Author {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;
	private String name;

	Author() {
	}

	public Author(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(store = Store.YES)
	@Field(name = "name_sort", analyze = Analyze.NO)
	@SortableField(forField = "name_sort")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
