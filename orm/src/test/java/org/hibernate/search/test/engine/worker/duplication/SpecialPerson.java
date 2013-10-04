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

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * Test entity for HSEARCH-257.
 *
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
@DiscriminatorValue("SpecialPerson")
public class SpecialPerson extends Person {

	@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@JoinColumn(name = "SPECIALPERSON_FK")
	@IndexedEmbedded
	private Set<EmailAddress> emailAddressSet = new HashSet<EmailAddress>();

	public Set<EmailAddress> getEmailAddressSet() {
		return emailAddressSet;
	}

	public void setEmailAddressSet(Set<EmailAddress> emailAddresses) {
		EmailAddress defaultVal = getDefaultEmailAddressFromList( emailAddresses );

		super.setDefaultEmailAddress( defaultVal );

		emailAddressSet = emailAddresses;
	}

	/**
	 * This function add the provided emailAddress to the existing set.
	 *
	 * @param emailAddress EmailAddress to add the the set
	 */
	public void addEmailAddress(EmailAddress emailAddress) {
		if ( emailAddress != null ) {
			if ( emailAddressSet == null ) {
				emailAddressSet = new HashSet<EmailAddress>();
			}

			// We cannot add another default address to the list. Check if
			// default
			// address has been set before.
			if ( emailAddress.isDefaultAddress() ) {
				// Replace old default address with new one.
				processDefaultEmailAddress( emailAddress, emailAddressSet );

				super.setDefaultEmailAddress( emailAddress );
			}
			else {
				emailAddressSet.add( emailAddress );
			}
		}
	}

	private void processDefaultEmailAddress(EmailAddress defaultVal,
											Set<EmailAddress> list) {
		if ( defaultVal != null ) {
			boolean addToList = true;

			for ( EmailAddress aList : list ) {

				if ( defaultVal.equals( aList ) ) {
					aList.setDefaultAddress( true );
					addToList = false;
				}
				else if ( aList.isDefaultAddress() ) {
					// Reset default value.
					aList.setDefaultAddress( false );
				}
			}

			// Add Email Address to the list if list does not contain it.
			if ( addToList ) {
				list.add( defaultVal );
			}
		}
	}

	private EmailAddress getDefaultEmailAddressFromList(
			Set<EmailAddress> list) {
		EmailAddress address = null;
		EmailAddress firstAddressInList = null;
		boolean found = false;

		if ( list != null ) {
			for ( EmailAddress aList : list ) {
				address = aList;

				if ( address != null ) {
					if ( firstAddressInList == null ) {
						firstAddressInList = address;
					}

					if ( address.isDefaultAddress() ) {
						found = true;
						break;
					}
				}
			}

			if ( !found && firstAddressInList != null ) {
				// If default address was not found we set the first one as
				// default.
				firstAddressInList.setDefaultAddress( true );
				address = firstAddressInList;
			}
		}

		return address;
	}
}
