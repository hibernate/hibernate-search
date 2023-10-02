/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.paths;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Entity
@Indexed
public class Book {

	@Id
	private Integer id;

	@FullTextField(analyzer = "english")
	private String title;

	@ManyToMany
	@JoinTable(name = "book_writer")
	@IndexedEmbedded(structure = ObjectStructure.NESTED)
	private List<Author> writers = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "book_artist")
	@IndexedEmbedded(structure = ObjectStructure.NESTED)
	private List<Author> artists = new ArrayList<>();

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

	public List<Author> getWriters() {
		return writers;
	}

	public void setWriters(List<Author> writers) {
		this.writers = writers;
	}

	public List<Author> getArtists() {
		return artists;
	}

	public void setArtists(List<Author> artists) {
		this.artists = artists;
	}
}
