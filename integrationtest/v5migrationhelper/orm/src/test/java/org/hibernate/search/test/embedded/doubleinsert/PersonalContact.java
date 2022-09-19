/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.doubleinsert;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

@Entity
@DiscriminatorValue("PersonalContact")
@Indexed
public class PersonalContact extends Contact {
	private static final long serialVersionUID = 1L;

	@Column(name = "P_FIRSTNAME")
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
	private String firstname;

	@Column(name = "P_SURNAME")
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
	private String surname;

	@Column(name = "P_DATEOFBIRTH")
	private Date dateOfBirth;

	@Column(name = "P_NOTIFYBIRTHDAY")
	private boolean notifyBirthDay;

	@Column(name = "P_MYFACESURL")
	@Field(store = Store.YES)
	private String myFacesUrl;

	@Column(name = "P_REMINDERCOUNT")
	private int reminderCount;

	@Column(name = "P_REMINDERRESET")
	private boolean reset;

	public PersonalContact() {
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public boolean isNotifyBirthDay() {
		return notifyBirthDay;
	}

	public void setNotifyBirthDay(boolean notifyBirthDay) {
		this.notifyBirthDay = notifyBirthDay;
	}

	public String getMyFacesUrl() {
		return myFacesUrl;
	}

	public void setMyFacesUrl(String myFacesUrl) {
		this.myFacesUrl = myFacesUrl;
	}

	public int getReminderCount() {
		return reminderCount;
	}

	public void setReminderCount(int reminderCount) {
		this.reminderCount = reminderCount;
	}

	public boolean isReset() {
		return reset;
	}

	public void setReset(boolean reset) {
		this.reset = reset;
	}
}
