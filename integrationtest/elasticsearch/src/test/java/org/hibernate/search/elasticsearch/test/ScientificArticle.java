/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;

/**
 * @author Richard Hallier
 */
@Entity
@Indexed
public class ScientificArticle {

	@Id
	@GeneratedValue
	@DocumentId
	private Long id;

	@Fields({
			@Field(name = "title", analyze = Analyze.NO),
			@Field(name = "titleAnalyzed")
	})
	@SortableField(forField = "title")
	private String title;

	@Field(name = "abstract")
	private String summary;

	@Field
	private String text;

	@Field
	private int wordCount;

	ScientificArticle() {
	}

	public ScientificArticle(String title, String summary, String text, int wordCount) {
		this.summary = summary;
		this.text = text;
		this.title = title;
		this.wordCount = wordCount;
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

	public int getWordCount() {
		return wordCount;
	}

	public void setWordCount(int wordCount) {
		this.wordCount = wordCount;
	}
}
