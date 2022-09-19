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
@org.hibernate.annotations.Proxy(lazy = true)
@org.hibernate.search.annotations.Indexed
@jakarta.persistence.Table(name = "entity1")
//@SequenceGenerator( name="ids_generator1", sequenceName = "ids_generator1")
public class Entity1ForDoc0 implements Serializable {

	private static final long serialVersionUID = -3191273589083411349L;

	@Id
	@Field
	@GeneratedValue //(generator = "ids_generator1", strategy = GenerationType.SEQUENCE)
	@Column(name = "universalid") //"uid" is a keywork in Oracle
	private long uid;

	@Version
	private int optlock;

	@jakarta.persistence.OneToMany(mappedBy = "entity1", cascade = { })
	@org.hibernate.search.annotations.IndexedEmbedded(includeEmbeddedObjectId = true)
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
