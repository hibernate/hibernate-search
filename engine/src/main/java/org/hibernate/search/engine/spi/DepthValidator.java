/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.engine.spi;

/**
 * Used to check the constraints of depth when using {@link org.hibernate.search.annotations.IndexedEmbedded}
 * or {@link org.hibernate.search.annotations.ContainedIn} annotations.
 *
 * @author Davide D'Alto
 */
public class DepthValidator {

	private final int maxDepth;
	private int depth;

	public DepthValidator(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public int getDepth() {
		return depth;
	}

	public void increaseDepth() {
		depth++;
	}

	public boolean isMaxDepthReached() {
		return depth > maxDepth;
	}

	public boolean isMaxDepthInfinite() {
		return maxDepth == Integer.MAX_VALUE;
	}

	@Override
	public String toString() {
		return "[maxDepth=" + maxDepth + ", level=" + depth + "]";
	}
}
