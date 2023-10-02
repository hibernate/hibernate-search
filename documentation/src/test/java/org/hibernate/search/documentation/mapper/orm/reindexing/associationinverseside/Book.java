/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.associationinverseside;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OrderBy;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
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
			inversePath = @ObjectPath(@PropertyValue(propertyName = "book"))
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
