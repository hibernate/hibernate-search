/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.doubleinsert;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Entity
@Table(name = "T_CONTACT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("Contact")
@DiscriminatorColumn(name = "contactType", discriminatorType = jakarta.persistence.DiscriminatorType.STRING)
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
	private Date createdOn;

	@Column(name = "C_LASTUPDATEDON")
	private Date lastUpdatedOn;

	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contact")))
	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
	private Set<Address> addresses;

	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contact")))
	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
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
