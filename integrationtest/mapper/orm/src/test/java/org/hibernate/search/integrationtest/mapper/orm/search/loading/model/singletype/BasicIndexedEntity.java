/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity(name = BasicIndexedEntity.NAME)
@NamedEntityGraph(
		name = BasicIndexedEntity.GRAPH_EAGER,
		includeAllAttributes = true
)
@NamedEntityGraph(
		name = BasicIndexedEntity.GRAPH_LAZY
)
public class BasicIndexedEntity {

	public static final String NAME = "indexed";

	public static final String GRAPH_EAGER = "graph-eager";
	public static final String GRAPH_LAZY = "graph-lazy";

	@Id
	private Integer id;

	private Integer uniqueProperty;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	private BasicContainedEntity containedEager;

	@OneToMany(mappedBy = "containingLazy", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private List<BasicContainedEntity> containedLazy = new ArrayList<>();

	protected BasicIndexedEntity() {
	}

	public BasicIndexedEntity(int id, Integer uniqueProperty) {
		this.id = id;
		this.uniqueProperty = uniqueProperty;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public BasicContainedEntity getContainedEager() {
		return containedEager;
	}

	public void setContainedEager(BasicContainedEntity containedEager) {
		this.containedEager = containedEager;
	}

	public List<BasicContainedEntity> getContainedLazy() {
		return containedLazy;
	}
}
