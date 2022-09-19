/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.sort;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Author {

	@Id
	private Integer id;

	@FullTextField(analyzer = "name")
	@KeywordField(name = "firstName_sort", normalizer = "name", sortable = Sortable.YES)
	private String firstName;

	@FullTextField(analyzer = "name")
	@KeywordField(name = "lastName_sort", normalizer = "name", sortable = Sortable.YES)
	private String lastName;

	@Embedded
	@GenericField(sortable = Sortable.YES)
	private EmbeddableGeoPoint placeOfBirth;

	@ManyToMany(mappedBy = "authors")
	@IndexedEmbedded(includeDepth = 1, structure = ObjectStructure.NESTED)
	private List<Book> books = new ArrayList<>();

	public Author() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public EmbeddableGeoPoint getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(EmbeddableGeoPoint placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	public List<Book> getBooks() {
		return books;
	}

	public void setBooks(List<Book> books) {
		this.books = books;
	}
}
