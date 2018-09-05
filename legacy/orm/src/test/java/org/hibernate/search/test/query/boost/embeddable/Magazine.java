/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost.embeddable;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class Magazine {

	@Id
	Long id;

	@Field
	private String description;

	@IndexedEmbedded(includePaths = { "value", "subTitle.value", "localizedTitle.value" })
	@Boost(4.0F) // rank title hits twice as important as description
	private Title title;

	Magazine() {
	}

	public Magazine(Long id, String description, Title title) {
		this.id = id;
		this.description = description;
		this.title = title;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Title getTitle() {
		return title;
	}

	public void setTitle(Title title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "Magazine [id=" + id + ", description=" + description + ", title=" + title + "]";
	}
}
