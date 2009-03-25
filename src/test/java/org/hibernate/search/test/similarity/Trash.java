// $Id$
package org.hibernate.search.test.similarity;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Similarity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@Similarity(impl = DummySimilarity.class)
public class Trash {
	@Id
	@DocumentId
	@GeneratedValue
	private Integer id;
	
	@Field
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
