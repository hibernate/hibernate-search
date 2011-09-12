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

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@DiscriminatorValue("BusinessContact")
@Indexed
public class BusinessContact extends Contact {

	@Column(name = "P_BUSINESSNAME")
	@Field(store = Store.YES)
	private String businessName;

	@Column(name = "P_BUSINESSURL")
	@Field(store = Store.YES)
	private String url;

	public BusinessContact() {
	}

	public String getBusinessName() {
		return businessName;
	}

	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	public String getUrl() {
		if ( null == this.url || "".equals( this.url ) ) {
			return "Not provided";
		}
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}

//	public boolean equals(Object object) {
//		if (!(object instanceof BusinessContact)) {
//			return false;
//		}
//		BusinessContact businessContact = (BusinessContact)object;
//		return new EqualsBuilder().append(new Object[]{this.getId(), this.getBusinessName(), this.getUrl()}, new Object[]{businessContact.getId(), businessContact.getBusinessName(), businessContact.getUrl()}).isEquals();
//	}
//
//	public int hashCode() {
//		return new HashCodeBuilder().append(new Object[]{new Long(this.getId()), this.getBusinessName(), this.getUrl()}).toHashCode();
//	}
// 	public String toString() {
//		StringBuilder buf = new StringBuilder();
//		buf.append("Business Name: " + this.getBusinessName() + Constants.NEW_LINE);
//		buf.append("Business Url: " +  this.getUrl() + Constants.NEW_LINE);
//		buf.append("Email: " + this.getEmail() + Constants.NEW_LINE);
//		super.displayPhonesAndAddresses(buf);
//		return buf.toString();
//	}
}
