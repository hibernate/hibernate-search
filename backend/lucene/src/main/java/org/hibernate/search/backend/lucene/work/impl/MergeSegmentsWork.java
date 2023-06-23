/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;


public class MergeSegmentsWork implements IndexManagementWork<Void> {
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Void execute(IndexManagementWorkExecutionContext context) {
		context.getIndexAccessor().mergeSegments();
		return null;
	}

	@Override
	public Object getInfo() {
		return this;
	}
}
