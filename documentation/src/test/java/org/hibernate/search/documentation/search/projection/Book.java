/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity(name = Book.NAME)
@Indexed
public class Book {

	public static final String NAME = "Book";

	@Id
	private Integer id;

	@FullTextField(analyzer = "english", projectable = Projectable.YES)
	private String title;

	@KeywordField(projectable = Projectable.YES)
	private Genre genre;

	@GenericField(projectable = Projectable.YES)
	private Integer pageCount;

	@FullTextField(analyzer = "english", projectable = Projectable.YES)
	private String description;

	@ManyToMany
	@IndexedEmbedded(structure = ObjectStructure.NESTED)
	private List<Author> authors = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "BOOK_AUTHOR_FLATTENED")
	@IndexedEmbedded(structure = ObjectStructure.FLATTENED)
	private List<Author> flattenedAuthors = new ArrayList<>();

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

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}

	public List<Author> getFlattenedAuthors() {
		return flattenedAuthors;
	}

	public void setFlattenedAuthors(List<Author> flattenedAuthors) {
		this.flattenedAuthors = flattenedAuthors;
	}

	public void addAuthor(Author author) {
		authors.add( author );
		flattenedAuthors.add( author );

		author.getBooks().add( this );
		author.getFlattenedBooks().add( this );
	}
}
