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
package org.hibernate.search.test.engine.worker.duplication;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

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

	@Field(name = "Content")
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
