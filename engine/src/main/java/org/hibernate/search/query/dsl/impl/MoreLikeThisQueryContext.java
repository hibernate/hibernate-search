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
public class MoreLikeThisQueryContext {
	private boolean boostTerms = false;
	private float termBoostFactor = 1f;
	private boolean excludeEntityUsedForComparison;

	public boolean isBoostTerms() {
		return boostTerms;
	}

	public void setBoostTerms(boolean boostTerms) {
		this.boostTerms = boostTerms;
	}

	public float getTermBoostFactor() {
		return termBoostFactor;
	}

	public void setTermBoostFactor(float termBoostFactor) {
		this.termBoostFactor = termBoostFactor;
	}

	public void setExcludeEntityUsedForComparison(boolean excludeEntityUsedForComparison) {
		this.excludeEntityUsedForComparison = excludeEntityUsedForComparison;
	}

	public boolean isExcludeEntityUsedForComparison() {
		return excludeEntityUsedForComparison;
	}
}
