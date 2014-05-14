/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
