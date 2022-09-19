/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.aggregation;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english")
	private String title;

	@GenericField(aggregable = Aggregable.YES)
	private Double price;

	@KeywordField(aggregable = Aggregable.YES)
	private Genre genre;

	// Using the legacy java.sql.Date type on purpose, to demonstrate how to ignore bridges
	@GenericField(aggregable = Aggregable.YES)
	private Date releaseDate;

	@OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
	@OrderColumn
	@IndexedEmbedded(structure = ObjectStructure.NESTED)
	private List<BookEdition> editions = new ArrayList<>();

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

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public List<BookEdition> getEditions() {
		return editions;
	}

	public void setEditions(List<BookEdition> editions) {
		this.editions = editions;
	}
}
