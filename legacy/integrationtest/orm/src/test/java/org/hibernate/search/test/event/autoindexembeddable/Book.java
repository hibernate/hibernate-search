/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.event.autoindexembeddable;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Book {

	private Long id;
	private String title;
	private EmbeddableCategories embeddableCategories;

	@Id
	@DocumentId
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(store = Store.YES)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Embedded
	@IndexedEmbedded
	public EmbeddableCategories getEmbeddableCategories() {
		if ( embeddableCategories == null ) {
			embeddableCategories = new EmbeddableCategories();
		}
		return embeddableCategories;
	}

	public void setEmbeddableCategories(EmbeddableCategories categories) {
		this.embeddableCategories = categories;
	}
}


