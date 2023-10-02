/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.containerextractor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderBy;

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
