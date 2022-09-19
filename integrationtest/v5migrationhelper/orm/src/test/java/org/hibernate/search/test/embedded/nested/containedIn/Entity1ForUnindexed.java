/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.search.annotations.Field;

/**
 * Entite 1
 *
 * @author grolland
 */
@jakarta.persistence.Entity
@org.hibernate.annotations.Proxy(lazy = false)
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@jakarta.persistence.Table(name = "entity1")
public class Entity1ForUnindexed implements Serializable {

	private static final long serialVersionUID = -3191273589083411349L;

	@Id
	@GeneratedValue
	@Field(name = "uid-numeric")
	@Column(name = "universalid")
	private long uid;

	@Version
	private int optlock;

	@jakarta.persistence.OneToMany(mappedBy = "entity1", cascade = { })
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
