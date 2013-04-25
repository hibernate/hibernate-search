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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author grolland
 */
@javax.persistence.Entity
@org.hibernate.annotations.Proxy(lazy = true)
@javax.persistence.Table(name = "entity2")
public class Entity2ForDoc0 {

	private static final long serialVersionUID = -3191273589083411349L;

	@Id
	@GeneratedValue //(generator = "ids_generator2", strategy = GenerationType.SEQUENCE)
	@Column(name = "universalid")//"uid" is a keywork in Oracle
	private long uid;

	@Version
	private int optlock;

	@javax.persistence.ManyToOne(cascade = { }, fetch = javax.persistence.FetchType.LAZY)
	@org.hibernate.search.annotations.ContainedIn
	private Entity1ForDoc0 entity1;

	@Basic
	private String name = null;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}


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

	public void setEntity1(final Entity1ForDoc0 entity1) {
		this.entity1 = entity1;
	}

	public Entity1ForDoc0 getEntity1() {
		return entity1;
	}
}
