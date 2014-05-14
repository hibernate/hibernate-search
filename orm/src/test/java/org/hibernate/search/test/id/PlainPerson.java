/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * Copied from Person, but omitting the Hibernate Search annotations
 * to test programmatic mapping instead.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@Entity
public class PlainPerson {

	@EmbeddedId
	private PersonPK id;
	private String favoriteColor;

	public PersonPK getId() {
		return id;
	}

	public void setId(PersonPK id) {
		this.id = id;
	}

	public String getFavoriteColor() {
		return favoriteColor;
	}

	public void setFavoriteColor(String favoriteColor) {
		this.favoriteColor = favoriteColor;
	}

}
