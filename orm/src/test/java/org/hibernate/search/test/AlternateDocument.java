/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
