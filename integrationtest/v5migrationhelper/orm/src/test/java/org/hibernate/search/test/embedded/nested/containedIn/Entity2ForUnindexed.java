/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Entite 2
 *
 * @author grolland
 */
@jakarta.persistence.Entity
@org.hibernate.annotations.Proxy(lazy = false)
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@jakarta.persistence.Table(name = "entity2")
@org.hibernate.search.annotations.Indexed
public class Entity2ForUnindexed {

	/**
	 * Identifiant unique
	 */
	@Id
	@GeneratedValue
	@Column(name = "universalid")// "uid" is a keyword in Oracle
	private long uid;

	/**
	 * Controle de version optimiste
	 */
	@Version
	private int optlock;

	@jakarta.persistence.ManyToOne(cascade = { }, fetch = jakarta.persistence.FetchType.LAZY)
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
