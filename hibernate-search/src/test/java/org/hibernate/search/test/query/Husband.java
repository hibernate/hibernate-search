package org.hibernate.search.test.query;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Husband {
	@Id @GeneratedValue
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	private Long id;

	@Field(index = Index.TOKENIZED, store = Store.YES)
	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }
	private String lastName;

	@ManyToOne(fetch = FetchType.LAZY)
	public Spouse getSpouse() { return spouse; }
	public void setSpouse(Spouse spouse) { this.spouse = spouse; }
	private Spouse spouse;
}
