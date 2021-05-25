/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.io.Serializable;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public final class PojoIndexingQueueEventPayload implements Serializable {

	public final DocumentRoutesDescriptor routes;
	public final UpdateCauseDescriptor updateCause;

	public PojoIndexingQueueEventPayload(DocumentRoutesDescriptor routes,
			UpdateCauseDescriptor updateCause) {
		this.routes = routes;
		this.updateCause = updateCause;
	}

	@Override
	public String toString() {
		return "PojoIndexingQueueEventPayload{" +
				"routes=" + routes +
				", updateCause=" + updateCause +
				'}';
	}
}
