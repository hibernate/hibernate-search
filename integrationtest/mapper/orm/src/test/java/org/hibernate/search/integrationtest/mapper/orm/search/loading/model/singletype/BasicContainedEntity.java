/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity(name = BasicContainedEntity.NAME)
public class BasicContainedEntity {

	public static final String NAME = "contained";

	@Id
	private Integer id;

	@OneToOne(mappedBy = "containedEager")
	private BasicIndexedEntity containingEager;

	@ManyToOne
	private BasicIndexedEntity containingLazy;

	protected BasicContainedEntity() {
	}

	public BasicContainedEntity(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

	public BasicIndexedEntity getContainingEager() {
		return containingEager;
	}

	public void setContainingEager(BasicIndexedEntity containingEager) {
		this.containingEager = containingEager;
	}

	public BasicIndexedEntity getContainingLazy() {
		return containingLazy;
	}

	public void setContainingLazy(BasicIndexedEntity containingLazy) {
		this.containingLazy = containingLazy;
	}
}
