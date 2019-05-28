/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

public class LuceneSucceedingCompatibilityChecker implements LuceneCompatibilityChecker {

	@Override
	public void failIfNotCompatible() {
		// this sub class never fails
	}

	@Override
	public LuceneCompatibilityChecker combine(LuceneCompatibilityChecker other) {
		// succeeding + any = any
		return other;
	}
}
