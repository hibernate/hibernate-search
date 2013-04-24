/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * Entite 1
 *
 * @author grolland
 */
@javax.persistence.Entity
@org.hibernate.annotations.Proxy(lazy = true)
@org.hibernate.search.annotations.Indexed
@javax.persistence.Table(name = "entity1")
//@SequenceGenerator( name="ids_generator1", sequenceName = "ids_generator1")
public class Entity1ForDoc0 implements Serializable {

	private static final long serialVersionUID = -3191273589083411349L;

	@Id
	@GeneratedValue //(generator = "ids_generator1", strategy = GenerationType.SEQUENCE)
	@Column(name = "universalid")//"uid" is a keywork in Oracle
	private long uid;

	@Version
	private int optlock;

	@javax.persistence.OneToMany(mappedBy = "entity1", cascade = { })
	@org.hibernate.search.annotations.IndexedEmbedded
	private java.util.List<Entity2ForDoc0> entities2 = new java.util.ArrayList<Entity2ForDoc0>();


	public long getUid() {
		return uid;
	}

	public void setUid(final long uid) {
		this.uid = uid;
	}

	public int getOptlock() {
		return optlock;
	}

	public void setOptlock(final int optlock) {
		this.optlock = optlock;
	}

	public void setEntities2(final java.util.List<Entity2ForDoc0> entities2) {
		this.entities2 = entities2;
	}

	public java.util.List<Entity2ForDoc0> getEntities2() {
		return entities2;
	}

}
