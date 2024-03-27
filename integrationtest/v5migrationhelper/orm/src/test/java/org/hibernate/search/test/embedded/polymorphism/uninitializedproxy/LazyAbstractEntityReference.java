/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.polymorphism.uninitializedproxy;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.DocumentId;

@Entity
@Cacheable
public class LazyAbstractEntityReference {

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	private AbstractEntity entity;

	protected LazyAbstractEntityReference() {
		// For Hibernate use only
	}

	public LazyAbstractEntityReference(AbstractEntity entity) {
		super();
		setEntity( entity );
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public AbstractEntity getEntity() {
		return entity;
	}

	public void setEntity(AbstractEntity entity) {
		this.entity = entity;
	}
}
