/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.query.dsl.embedded;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author neek
 */
@Entity
@Indexed
public class ContainerEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Field
	private String parentStringValue;

	@Embedded
	@IndexedEmbedded(depth = 1, prefix = "emb.")
	private EmbeddedEntity embeddedEntity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getParentStringValue() {
		return parentStringValue;
	}

	public void setParentStringValue(String parentStringValue) {
		this.parentStringValue = parentStringValue;
	}

	public EmbeddedEntity getEmbeddedEntity() {
		return embeddedEntity;
	}

	public void setEmbeddedEntity(EmbeddedEntity embeddedEntity) {
		this.embeddedEntity = embeddedEntity;
	}

}
