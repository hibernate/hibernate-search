package org.hibernate.search.test.configuration.mutablefactory;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Indexed
public class A {
	private Integer id;
	private String name;

	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
