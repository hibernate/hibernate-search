/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.analysis;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A descriptor of a normalizer, exposing in particular the name of the normalizer.
 */
@Incubating
public interface NormalizerDescriptor extends AnalysisDescriptor {
}
