/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.analysis;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A descriptor of an abstract analysis concept.
 *
 * @see AnalyzerDescriptor
 * @see NormalizerDescriptor
 */
@Incubating
public interface AnalysisDescriptor {

	/**
	 * @return The name that identifies the concept behind this descriptor.
	 */
	String name();
}
