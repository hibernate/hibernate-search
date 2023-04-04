/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.highlighting;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::basics[]
@Entity(name = Book.NAME)
@Indexed
public class Book {

	public static final String NAME = "Book";

	@Id
	private Integer id;

	@FullTextField(analyzer = "english", projectable = Projectable.YES,
			highlightable = { Highlightable.PLAIN, Highlightable.UNIFIED }) // <1>
	private String title;

	@FullTextField(analyzer = "english", projectable = Projectable.YES, highlightable = Highlightable.ANY) // <2>
	@Column(length = 10000)
	private String description;

	// end::basics[]
	public Book() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
// tag::basics[]
}
// end::basics[]
