/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;

public interface Dataset {

	void populate(MappedIndex index, DocumentElement documentElement, long documentId, long randomizer);

}
