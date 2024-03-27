/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.update;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Entity
public class Dad {
	private Long id;
	private String name;
	private Grandpa grandpa;
	private Set<Son> sons = new HashSet<Son>();

	public Dad() {
	}

	public Dad(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	public Grandpa getGrandpa() {
		return grandpa;
	}

	public void setGrandpa(Grandpa grandpa) {
		this.grandpa = grandpa;
	}

	@Field(store = Store.YES)
	@Transient
	@IndexingDependency(derivedFrom = @ObjectPath({
			@PropertyValue(propertyName = "grandpa"),
			@PropertyValue(propertyName = "id")
	}))
	public Long getGrandpaId() {
		return grandpa != null ? grandpa.getId() : null;
	}

	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "dad")))
	@OneToMany
	public Set<Son> getSons() {
		return sons;
	}

	public void setSons(Set<Son> sons) {
		this.sons = sons;
	}

	public boolean add(Son son) {
		son.setDad( this );
		return sons.add( son );
	}
}

