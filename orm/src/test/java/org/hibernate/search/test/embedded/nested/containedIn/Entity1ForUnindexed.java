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

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;

/**
 * Entite 1
 *
 * @author grolland
 */
@javax.persistence.Entity
@org.hibernate.annotations.Proxy(lazy = false)
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@javax.persistence.Table(name = "entity1")
public class Entity1ForUnindexed implements Serializable {

	private static final long serialVersionUID = -3191273589083411349L;

	@Id
	@GeneratedValue
	@Field(analyze = Analyze.NO)
	@Column(name = "universalid")
	private long uid;

	@Version
	private int optlock;

	@javax.persistence.OneToMany(mappedBy = "entity1", cascade = { })
	@org.hibernate.search.annotations.ContainedIn
	@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
	private java.util.List<Entity2ForUnindexed> entities2 = new java.util.ArrayList<Entity2ForUnindexed>();


	public long getUid() {
		return uid;
	}

	/**
	 * Setter de l'attribut uid
	 *
	 * @param uid uid a definir.
	 */
	public void setUid(long uid) {
		this.uid = uid;
	}

	/**
	 * Getter de l'attribut optlock
	 *
	 * @return int : Renvoie optlock.
	 */
	public int getOptlock() {
		return optlock;
	}

	/**
	 * Setter de l'attribut optlock
	 *
	 * @param optlock optlock a definir.
	 */
	public void setOptlock(int optlock) {
		this.optlock = optlock;
	}

	/**
	 * Setter de l'attribut entities2
	 *
	 * @param entities2 entities2 a definir.
	 */
	public void setEntities2(java.util.List<Entity2ForUnindexed> entities2) {
		this.entities2 = entities2;
	}

	/**
	 * Getter de l'attribut entities2
	 *
	 * @return java.util.List<Entity2> : Renvoie entities2.
	 */
	public java.util.List<Entity2ForUnindexed> getEntities2() {
		return entities2;
	}

}
