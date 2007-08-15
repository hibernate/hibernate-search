//$Id$
package org.hibernate.search.test.query;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author John Grffin
 */
@Entity
@Indexed
public class Employee {
	private Integer id;
	private String lastname;
	private String dept;

	public Employee() {
	}

	public Employee(Integer id, String lastname, String dept) {
		this.id = id;
		this.lastname = lastname;
		this.dept = dept;
	}

	@Id
	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field( index = Index.NO, store = Store.YES )
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	@Field( index = Index.TOKENIZED, store = Store.YES )
	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}
}
