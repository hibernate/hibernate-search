/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed(index = "Documents")
public class Document {
	private Long id;
	private String title;
	private String summary;
	private String text;

	public Document() {
	}

	public Document(String title, String summary, String text) {
		super();
		this.summary = summary;
		this.text = text;
		this.title = title;
	}

	@Id
	@GeneratedValue
	@DocumentId
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(store = Store.YES)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Field(name = "Abstract")
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Field
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
