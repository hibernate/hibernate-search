// $Id:$
package org.hibernate.search.test.worker.duplication;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.OneToOne;
import javax.persistence.JoinColumn;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * Test entity for HSEARCH-257.
 *
 * @author Marina Vatkina
 * @author Hardy Ferentschik
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DISC", discriminatorType = DiscriminatorType.STRING)
public class Person {

	@Id
	@GeneratedValue
	@DocumentId
	private int id;

	@Field(index = Index.TOKENIZED, name = "Content")
	private String name;

	@OneToOne(fetch = FetchType.EAGER, cascade = {
			CascadeType.MERGE,
			CascadeType.PERSIST
	})
	@JoinColumn(name = "DEFAULT_EMAILADDRESS_FK")
	private EmailAddress defaultEmailAddress;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * This function return the value of defaultEmailAddress.
	 *
	 * @return the defaultEmailAddress
	 */

	public EmailAddress getDefaultEmailAddress() {
		return defaultEmailAddress;
	}

	/**
	 * This function sets the value of the defaultEmailAddress.
	 *
	 * @param defaultEmailAddress the defaultEmailAddress to set
	 */
	protected void setDefaultEmailAddress(EmailAddress defaultEmailAddress) {
		this.defaultEmailAddress = defaultEmailAddress;
	}
}
