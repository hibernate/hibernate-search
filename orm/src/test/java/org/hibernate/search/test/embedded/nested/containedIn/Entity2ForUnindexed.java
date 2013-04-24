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

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * Entite 2
 *
 * @author grolland
 */
@javax.persistence.Entity
@org.hibernate.annotations.Proxy(lazy = false)
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@javax.persistence.Table(name = "entity2")
@org.hibernate.search.annotations.Indexed
public class Entity2ForUnindexed {
	/**
	 * Commentaire pour <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -3191273589083411349L;

	/**
	 * Identifiant unique
	 */
	@Id
	@GeneratedValue
	@Column(name = "universalid")// "uid" is a keywork in Oracle
	private long uid;

	/**
	 * Controle de version optimiste
	 */
	@Version
	private int optlock;

	@javax.persistence.ManyToOne(cascade = { }, fetch = javax.persistence.FetchType.LAZY)
	@org.hibernate.search.annotations.IndexedEmbedded()
	@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
	private Entity1ForUnindexed entity1;

	/**
	 * Getter de l'attribut uid
	 *
	 * @return long : Renvoie uid.
	 */

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
	 * Setter de l'attribut entity1
	 *
	 * @param entity1 entity1 a definir.
	 */
	public void setEntity1(Entity1ForUnindexed entity1) {
		this.entity1 = entity1;
	}

	/**
	 * Getter de l'attribut entity1
	 *
	 * @return Entity1 : Renvoie entity1.
	 */
	public Entity1ForUnindexed getEntity1() {
		return entity1;
	}
}
