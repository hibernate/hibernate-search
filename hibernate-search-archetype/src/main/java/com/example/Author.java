// $Id: com.example.Author.java 17630 2009-10-06 13:38:43Z sannegrinovero $
package com.example;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

@Entity
public class Author {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(index = Index.TOKENIZED, store = Store.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
