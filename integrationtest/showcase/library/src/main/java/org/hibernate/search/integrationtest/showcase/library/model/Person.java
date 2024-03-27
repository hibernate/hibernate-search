/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import java.util.StringJoiner;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Person extends AbstractEntity<Integer> {

	@Id
	private Integer id;

	@Basic(optional = false)
	@FullTextField
	@KeywordField(
			name = "firstName_sort",
			normalizer = LibraryAnalyzers.NORMALIZER_SORT,
			sortable = Sortable.YES
	)
	private String firstName;

	@Basic(optional = false)
	@FullTextField
	@KeywordField(
			name = "lastName_sort",
			normalizer = LibraryAnalyzers.NORMALIZER_SORT,
			sortable = Sortable.YES
	)
	private String lastName;

	@OneToOne(mappedBy = "user")
	@IndexedEmbedded
	private Account account;

	public Person() {
	}

	public Person(int id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
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

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	@Override
	protected String getDescriptionForToString() {
		return new StringJoiner( "," )
				.add( getLastName() )
				.add( getFirstName() )
				.toString();
	}
}
