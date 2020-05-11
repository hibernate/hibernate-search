/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.associationinverseside;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OrderBy;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
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

	@FullTextField(analyzer = "name")
	private String title;

	@ElementCollection // <1>
	@JoinTable(
			name = "book_editionbyprice",
			joinColumns = @JoinColumn(name = "book_id")
	)
	@MapKeyJoinColumn(name = "edition_id")
	@Column(name = "price")
	@OrderBy("edition_id asc")
	@IndexedEmbedded( // <2>
			name = "editionsForSale",
			extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
	)
	@AssociationInverseSide( // <3>
			extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY),
			inversePath = @ObjectPath( @PropertyValue( propertyName = "book" ) )
	)
	private Map<BookEdition, BigDecimal> priceByEdition = new LinkedHashMap<>();

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

	public Map<BookEdition, BigDecimal> getPriceByEdition() {
		return priceByEdition;
	}

	public void setPriceByEdition(Map<BookEdition, BigDecimal> priceByEdition) {
		this.priceByEdition = priceByEdition;
	}
	//end::getters-setters[]
}
//end::include[]
