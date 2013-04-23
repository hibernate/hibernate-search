/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.embedded.doubleinsert;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@Table(name = "T_CONTACT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("Contact")
@DiscriminatorColumn(name = "contactType", discriminatorType = javax.persistence.DiscriminatorType.STRING)
@Indexed
public class Contact implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "C_CONTACT_ID")
	@DocumentId
	private long id;

	@Column(name = "C_EMAIL")
	@Field(store = Store.YES)
	private String email;

	@Column(name = "C_CREATEDON")
	@Type(type = "java.util.Date")
	private Date createdOn;

	@Column(name = "C_LASTUPDATEDON")
	@Type(type = "java.util.Date")
	private Date lastUpdatedOn;

	@ContainedIn
	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
	@Type(type = "java.util.Set")
	private Set<Address> addresses;

	@ContainedIn
	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
	@Type(type = "java.util.Set")
	private Set<Phone> phoneNumbers;

	@Column(name = "C_NOTES")
	private String notes;

	public Contact() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getEmail() {
		if ( null == this.email || "".equals( this.email ) ) {
			return "N/A";
		}
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Date getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Date lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}

	public Set<Phone> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(Set<Phone> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public void addAddressToContact(Address address) {
		if ( address == null ) {
			throw new IllegalArgumentException( "Address cannot be null" );
		}
		if ( addresses == null ) {
			addresses = new HashSet<Address>();
		}
		address.setContact( this );
		addresses.add( address );
	}


	public void addPhoneToContact(Phone phone) {
		if ( phone == null ) {
			throw new IllegalArgumentException( "Phone cannot be null" );
		}
		if ( phoneNumbers == null ) {
			phoneNumbers = new HashSet<Phone>();
		}
		phone.setContact( this );
		phoneNumbers.add( phone );
	}


	public void removePhoneFromContact(Phone phone) {
		if ( phone == null ) {
			throw new IllegalArgumentException( "Phone cannot be null" );
		}
		if ( this.phoneNumbers.contains( phone ) ) {
			this.phoneNumbers.remove( phone );
		}
	}

	public void removeAddressFromContact(Address address) {
		if ( address == null ) {
			throw new IllegalArgumentException( "Address cannot be null" );
		}
		if ( this.addresses.contains( address ) ) {
			this.addresses.remove( address );
		}
	}
}
