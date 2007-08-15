//$Id$
package org.hibernate.search.test.fieldAccess;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.hibernate.search.annotations.Unstored;
import org.hibernate.search.annotations.Text;
import org.hibernate.search.annotations.Keyword;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * @author Richard Hallier
 */
@Entity
@Indexed(index = "DocumentField")
public class Document {
	@Id
    @GeneratedValue
    @Keyword(id = true)
	private Long id;

	@Field(index = Index.TOKENIZED)
	@Boost(2)
	private String title;

	@Unstored(name = "Abstract")
	private String summary;

	@Lob
    @Unstored
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

