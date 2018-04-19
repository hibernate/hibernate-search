/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend;

import org.hibernate.search.engine.backend.spi.BackendExtension;

public interface Backend {

	// TODO add standard APIs related to analysis (which is backend-scoped). To test if an analyzer is defined, for example.
	// TODO add standard APIs related to statistics?
	// TODO add other standard backend APIs?

	default <T extends Backend> T withExtension(BackendExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
