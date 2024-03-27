/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.doubleinsert;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed
@Table(name = "T_ADDRESS")
public class Address implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "A_ADDRESS_ID")
	@DocumentId
	private long id;

	@Column(name = "A_ADDRESS1")
	@Field(store = Store.YES)
	private String address1;

	@Column(name = "A_ADDRESS2")
	@Field(store = Store.YES)
	private String address2;

	@Column(name = "A_TOWN")
	@Field(store = Store.YES)
	private String town;

	@Column(name = "A_COUNTY")
	@Field(store = Store.YES)
	private String county;

	@Column(name = "A_COUNTRY")
	@Field(store = Store.YES)
	private String country;

	@Column(name = "A_POSTCODE")
	@Field(store = Store.YES)
	private String postcode;

	@Column(name = "A_ACTIVE")
	private boolean active;

	@Column(name = "A_CREATEDON")
	private Date createdOn;

	@Column(name = "A_LASTUPDATEDON")
	private Date lastUpdatedOn;

	@ManyToOne
	@JoinColumn(name = "C_CONTACT_ID")
	@IndexedEmbedded
	private Contact contact;

	public Address(String address1, String address2, String town, String county, String country, String postcode,
			boolean active, Contact contact) {
		super();
		this.address1 = address1;
		this.address2 = address2;
		this.town = town;
		this.county = county;
		this.country = country;
		this.postcode = postcode;
		this.active = active;
		this.contact = contact;
	}

	public Address() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public String getAddress2() {
		if ( null == this.address2 || "".equals( this.address2 ) ) {
			return "N/A";
		}
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getTown() {
		return town;
	}

	public void setTown(String town) {
		this.town = town;
	}

	public String getCounty() {
		if ( null == this.county || "".equals( this.county ) ) {
			return "N/A";
		}
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
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

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	@Override
	public boolean equals(Object object) {
		if ( !( object instanceof Address ) ) {
			return false;
		}
		Address that = (Address) object;
		if ( !equals( this.getAddress1(), that.getAddress1() ) ) {
			return false;
		}
		if ( !equals( this.getAddress2(), that.getAddress2() ) ) {
			return false;
		}
		if ( !equals( this.getCounty(), that.getCounty() ) ) {
			return false;
		}
		if ( !equals( this.getTown(), that.getTown() ) ) {
			return false;
		}
		if ( !equals( this.getPostcode(), that.getPostcode() ) ) {
			return false;
		}
		return equals( this.getContact(), that.getContact() );
		//		EqualsBuilder equalsBuilder = new EqualsBuilder();
		//		return equalsBuilder.append(new Object[]{this.getAddress1(), this.getAddress2(), this.getCounty(), this.getTown(), this.getPostcode(), this.contact}, new Object[]{address.getAddress1(), address.getAddress2(), address.getCounty(), address.getTown(), address.getPostcode(), address.getContact()}).isEquals();
	}

	private boolean equals(Object o1, Object o2) {
		if ( o1 == o2 ) {
			return true;
		}
		if ( o1 == null || o2 == null ) {
			return false;
		}
		return o1.equals( o2 );
	}

	private int hashCode(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public int hashCode() {
		int a = 13;
		a = a * 23 + hashCode( this.getAddress1() );
		a = a * 23 + hashCode( this.getAddress2() );
		a = a * 23 + hashCode( this.getCounty() );
		a = a * 23 + hashCode( this.getTown() );
		a = a * 23 + hashCode( this.getPostcode() );
		a = a * 23 + hashCode( this.getContact() );
		return a;
		//		return new HashCodeBuilder().append(new Object[]{this.getAddress1(), this.getAddress2(), this.getCounty(), this.getTown(), this.getPostcode(), this.getContact()}).hashCode();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		displayAddress( buf, this );
		return buf.toString();
	}

	private void displayAddress(StringBuilder buf, Address address) {
		//		buf.append(Constants.TAB + Constants.TAB + "Address 1: " + address.getAddress1() + Constants.NEW_LINE);
		//		buf.append(Constants.TAB + Constants.TAB +"Address 2: " + address.getAddress2() + Constants.NEW_LINE);
		//		buf.append(Constants.TAB + Constants.TAB +"Town: " + address.getTown() + Constants.NEW_LINE);
		//		buf.append(Constants.TAB + Constants.TAB +"County: " + address.getCounty() + Constants.NEW_LINE);
		//		buf.append(Constants.TAB + Constants.TAB +"Postcode: " + address.getPostcode() + Constants.NEW_LINE);
		//		buf.append(Constants.TAB + Constants.TAB +"Country: " + address.getCountry() + Constants.NEW_LINE);
		//		buf.append(Constants.TAB + Constants.TAB +"Is current: " + (address.isActive()? "Yes" : "No") + Constants.NEW_LINE);
		//		buf.append(Constants.NEW_LINE);
	}

	public boolean isValidPostcode() {
		return false;
	}
}
