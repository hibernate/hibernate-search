/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.bridge.PaddedIntegerBridge;

/**
 * @author John Griffin
 */
@Entity
@Indexed
public class Employee {
	private Integer id;
	private String lastname;
	private String dept;

	@Field(store = Store.YES, analyze = Analyze.NO)
	@DateBridge(resolution = Resolution.DAY)
	public Date getHireDate() {
		return hireDate;
	}

	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}

	private Date hireDate;

	public Employee() {
	}

	public Employee(Integer id, String lastname, String dept) {
		this.id = id;
		this.lastname = lastname;
		this.dept = dept;
	}

	@Id
	@DocumentId
	@FieldBridge(impl = PaddedIntegerBridge.class) // test needs to sort on the id
	@SortableField
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(index = Index.NO, store = Store.YES)
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	@Field(store = Store.YES)
	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}
}
