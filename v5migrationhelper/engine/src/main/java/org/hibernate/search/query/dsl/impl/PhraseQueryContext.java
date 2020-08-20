/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

/**
 * @author Emmanuel Bernard
 */
public class PhraseQueryContext {
	private int slop = 0;
	private String sentence;

	public int getSlop() {
		return slop;
	}

	public void setSlop(int slop) {
		this.slop = slop;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
}
