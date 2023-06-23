/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.containerextractor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderBy;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	private String title;

	// tag::no-extractor[]
	@ManyToMany
	@GenericField( // <1>
			name = "authorCount",
			valueBridge = @ValueBridgeRef(type = MyCollectionSizeBridge.class), // <2>
			extraction = @ContainerExtraction(extract = ContainerExtract.NO) // <3>
	)
	private List<Author> authors = new ArrayList<>();
	// end::no-extractor[]

	// tag::explicit-extractor[]
	@ElementCollection // <1>
	@JoinTable(name = "book_pricebyformat")
	@MapKeyColumn(name = "format")
	@Column(name = "price")
	@OrderBy("format asc")
	@GenericField( // <2>
			name = "availableFormats",
			extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY) // <3>
	)
	private Map<BookFormat, BigDecimal> priceByFormat = new LinkedHashMap<>();
	// end::explicit-extractor[]

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

	public List<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}

	public Map<BookFormat, BigDecimal> getPriceByFormat() {
		return priceByFormat;
	}

	public void setPriceByFormat(Map<BookFormat, BigDecimal> priceByFormat) {
		this.priceByFormat = priceByFormat;
	}
}
