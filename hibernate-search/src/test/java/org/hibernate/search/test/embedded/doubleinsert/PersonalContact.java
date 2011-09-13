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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
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
	@Type(type = "java.util.Date")
	private Date dateOfBirth;

	@Column(name = "P_NOTIFYBIRTHDAY")
	@Type(type = "boolean")
	private boolean notifyBirthDay;

	@Column(name = "P_MYFACESURL")
	@Field(store = Store.YES)
	private String myFacesUrl;

	@Column(name = "P_REMINDERCOUNT")
	private int reminderCount;

	@Column(name = "P_REMINDERRESET")
	@Type(type = "boolean")
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
