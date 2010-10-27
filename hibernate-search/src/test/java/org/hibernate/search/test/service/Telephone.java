package org.hibernate.search.test.service;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Telephone {
	@Id
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;

	@Field(index = Index.TOKENIZED)
	public String getNumber() { return number; }
	public void setNumber(String number) {  this.number = number; }
	private String number;
}
