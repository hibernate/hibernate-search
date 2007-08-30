//$Id$
package org.hibernate.search.test.fieldAccess;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Richard Hallier
 */
@Entity
@Indexed(index = "DocumentField")
public class Document {
	@Id
    @GeneratedValue
    @DocumentId
	private Long id;

	@Field(index = Index.TOKENIZED)
	@Boost(2)
	private String title;

	@Field(name="Abstract", index=Index.TOKENIZED, store= Store.NO)
	private String summary;

	@Lob
    @Field(index=Index.TOKENIZED, store=Store.NO)
	private String text;

	Document() {
	}

	public Document(String title, String summary, String text) {
		super();
		this.summary = summary;
		this.text = text;
		this.title = title;
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

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}

