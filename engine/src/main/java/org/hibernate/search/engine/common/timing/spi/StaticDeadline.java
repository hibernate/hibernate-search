/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.timing.spi;

final class StaticDeadline implements Deadline {

	private final long remainingTimeMillis;

	StaticDeadline(long remainingTimeMillis) {
		this.remainingTimeMillis = remainingTimeMillis;
	}

	@Override
	public long remainingTimeMillis() {
		return remainingTimeMillis;
	}
}
