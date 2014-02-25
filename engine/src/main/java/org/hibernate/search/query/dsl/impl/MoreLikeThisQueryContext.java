/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.query.dsl.impl;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
