/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.spi;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;

public class PojoModelPathFormatter {

	private final String formatted;

	public PojoModelPathFormatter(PojoModelPath pojoModelPath) {
		this.formatted = pojoModelPath.toPathString();
	}

	@Override
	public String toString() {
		return formatted;
	}
}
