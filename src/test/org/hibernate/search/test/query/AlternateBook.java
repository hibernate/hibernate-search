//$Id$
package org.hibernate.search.test.query;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index="Book")
public class AlternateBook {
	@Id @DocumentId
	private Integer id;
	@Field(index = Index.TOKENIZED)
	private String summary;


	public AlternateBook() {
	}

	public AlternateBook(Integer id, String summary) {
		this.id = id;
		this.summary = summary;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
}
