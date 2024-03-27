/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.analysis.spi;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface AnalysisDescriptorRegistry {

	/**
	 * Looks up the configured analyzers.
	 *
	 * @param name The name of the analyzer.
	 * @return An {@link Optional#empty() empty optional} if there is no analyzer configured with the given name.
	 */
	Optional<? extends AnalyzerDescriptor> analyzerDescriptor(String name);

	/**
	 * @return A collection of configured analyzer descriptors.
	 */
	Collection<? extends AnalyzerDescriptor> analyzerDescriptors();

	/**
	 * Looks up the configured normalizers.
	 *
	 * @param name The name of the normalizer.
	 * @return An {@link Optional#empty() empty optional} if there is no normalizer configured with the given name.
	 */
	Optional<? extends NormalizerDescriptor> normalizerDescriptor(String name);

	/**
	 * @return A collection of configured normalizer descriptors.
	 */
	Collection<? extends NormalizerDescriptor> normalizerDescriptors();
}
