/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.math.BigInteger;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Field;

/**
 * @author Gunnar Morling
 */
@Embeddable
public class Ranking {

	@Field
	private BigInteger value;

	Ranking() {
	}

	public Ranking(BigInteger value) {
		this.value = value;
	}

	public BigInteger getValue() {
		return value;
	}

	public void setValue(BigInteger value) {
		this.value = value;
	}
}
