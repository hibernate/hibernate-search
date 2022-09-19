/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Book {

	@Id
	private MyProductId id;

	@GenericField(projectable = Projectable.YES)
	private ISBN isbn;

	@FullTextField(analyzer = "english")
	private String title;

	@FullTextField(analyzer = "english", projectable = Projectable.YES)
	private Genre genre;

	@GenericField
	private MyCoordinates location;

	public Book() {
	}

	public MyProductId getId() {
		return id;
	}

	public void setId(MyProductId id) {
		this.id = id;
	}

	public ISBN getIsbn() {
		return isbn;
	}

	public void setIsbn(ISBN isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public MyCoordinates getLocation() {
		return location;
	}

	public void setLocation(MyCoordinates location) {
		this.location = location;
	}
}
