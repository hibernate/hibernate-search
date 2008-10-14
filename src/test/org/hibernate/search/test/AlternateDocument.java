//$Id$
package org.hibernate.search.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Boost;

/**
 * Example of 2 entities mapped in the same index
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "Documents")
public class AlternateDocument {
	private Long id;
	private String title;
	private String summary;
	private String text;

	AlternateDocument() {
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

	@Field( name = "alt_title", store = Store.YES, index = Index.TOKENIZED )
	@Boost(2)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Field( name="Abstract", store = Store.NO, index = Index.TOKENIZED )
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Lob
	@Field( store = Store.NO, index = Index.TOKENIZED )
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}

