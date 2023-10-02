/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;

import org.openjdk.jmh.annotations.CompilerControl;

@CompilerControl(CompilerControl.Mode.INLINE)
final class ConstantDataset implements Dataset {

	@Override
	public void populate(MappedIndex index, DocumentElement documentElement, long documentId, long randomizer) {
		index.populate(
				documentElement,
				"Some short text " + randomizer,
				"Some very long text should be stored here. No, I mean long as in a book. " + randomizer,
				documentId + randomizer
		);
	}

}
