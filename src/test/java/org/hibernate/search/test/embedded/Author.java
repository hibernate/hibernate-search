//$Id$
package org.hibernate.search.test.embedded;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.DocumentId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Author {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;
	@Field(index= Index.TOKENIZED)
	private String name;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
