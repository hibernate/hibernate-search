/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;

public interface Dataset {

	void populate(MappedIndex index, DocumentElement documentElement, long documentId, long randomizer);

}
