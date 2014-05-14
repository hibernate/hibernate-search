/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * Entity to verify for HSEARCH-1050 : Having an EmbeddedId but override
 * the index id with a DocumentId.
 * Renaming the doc-id field as "id" just to make it more tricky.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@Entity
@Indexed
public class PersonCustomDocumentId {

	private PersonPK personNames;
	private String favoriteColor;
	private String securityNumber;

	@EmbeddedId
	public PersonPK getPersonNames() {
		return personNames;
	}

	public void setPersonNames(PersonPK personNames) {
		this.personNames = personNames;
	}

	@DocumentId(name = "id")
	public String getSecurityNumber() {
		return securityNumber;
	}

	public void setSecurityNumber(String securityNumber) {
		this.securityNumber = securityNumber;
	}

	@Field
	public String getFavoriteColor() {
		return favoriteColor;
	}

	public void setFavoriteColor(String favoriteColor) {
		this.favoriteColor = favoriteColor;
	}

}
