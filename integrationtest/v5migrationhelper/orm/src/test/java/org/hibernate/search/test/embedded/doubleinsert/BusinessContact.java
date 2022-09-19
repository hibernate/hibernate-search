/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.doubleinsert;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

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
