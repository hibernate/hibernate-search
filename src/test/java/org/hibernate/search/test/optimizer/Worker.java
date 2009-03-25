//$Id$
package org.hibernate.search.test.optimizer;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Worker {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;
	@Field(index = Index.TOKENIZED)
	private String name;
	@Field(index = Index.UN_TOKENIZED)
	private int workhours;


	public Worker() {
	}

	public Worker(String name, int workhours) {
		this.name = name;
		this.workhours = workhours;
	}

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

	public int getWorkhours() {
		return workhours;
	}

	public void setWorkhours(int workhours) {
		this.workhours = workhours;
	}
}
