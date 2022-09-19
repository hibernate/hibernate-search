/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.derivedfrom;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

// Note: this example is too complex for its own good, but I can't think of a much simpler one.
//tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	private String title;

	@ElementCollection
	private List<String> authors = new ArrayList<>(); // <1>

	public Book() {
	}

	// Getters and setters
	// ...
	//tag::getters-setters[]

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

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}
	//end::getters-setters[]

	@Transient // <2>
	@FullTextField(analyzer = "name") // <3>
	@IndexingDependency(derivedFrom = @ObjectPath({ // <4>
			@PropertyValue(propertyName = "authors")
	}))
	public String getMainAuthor() {
		return authors.isEmpty() ? null : authors.get( 0 );
	}
}
//end::include[]
