/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import javax.persistence.Entity;
import javax.persistence.EmbeddedId;

import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Person {
	@EmbeddedId
	@FieldBridge(impl = PersonPKBridge.class)
	@DocumentId
	private PersonPK id;
	private String favoriteColor;

	public PersonPK getId() {
		return id;
	}

	public void setId(PersonPK id) {
		this.id = id;
	}

	@Field
	public String getFavoriteColor() {
		return favoriteColor;
	}

	public void setFavoriteColor(String favoriteColor) {
		this.favoriteColor = favoriteColor;
	}
}
