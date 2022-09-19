/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.structure.flattened;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

// tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english")
	private String title;

	@ManyToMany
	@IndexedEmbedded(structure = ObjectStructure.FLATTENED) // <1>
	private List<Author> authors = new ArrayList<>();

	public Book() {
	}

	// Getters and setters
	// ...

	// tag::getters-setters[]
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

	public List<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}
	// end::getters-setters[]
}
// end::include[]
