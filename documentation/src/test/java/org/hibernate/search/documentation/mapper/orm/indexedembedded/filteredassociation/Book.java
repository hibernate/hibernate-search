/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.filteredassociation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

//tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english")
	private String title;

	@OneToMany(mappedBy = "book")
	@OrderBy("id asc")
	private List<BookEdition> editions = new ArrayList<>(); // <1>

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

	public List<BookEdition> getEditions() {
		return editions;
	}

	public void setEditions(List<BookEdition> editions) {
		this.editions = editions;
	}
	//end::getters-setters[]

	@Transient // <2>
	@IndexedEmbedded // <3>
	@AssociationInverseSide(inversePath = @ObjectPath( // <4>
			@PropertyValue(propertyName = "book")
	))
	@IndexingDependency(derivedFrom = @ObjectPath({ // <5>
			@PropertyValue(propertyName = "editions"),
			@PropertyValue(propertyName = "status")
	}))
	public List<BookEdition> getEditionsNotRetired() {
		return editions.stream()
				.filter( e -> e.getStatus() != BookEdition.Status.RETIRED )
				.collect( Collectors.toList() );
	}
}
//end::include[]
