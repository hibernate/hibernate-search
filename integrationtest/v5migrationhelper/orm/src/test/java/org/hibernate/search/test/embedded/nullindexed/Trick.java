/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.nullindexed;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Field;

/**
 * @author Yoann Rodiere
 */
@Embeddable
public class Trick {

	@Field
	private String name;

	@Field
	private String reward;

	public Trick() {
	}

	public Trick(String name, String reward) {
		this.name = name;
		this.reward = reward;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReward() {
		return reward;
	}

	public void setReward(String reward) {
		this.reward = reward;
	}

}
