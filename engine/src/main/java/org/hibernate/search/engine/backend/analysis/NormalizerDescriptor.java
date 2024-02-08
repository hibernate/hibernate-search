/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.analysis;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A descriptor of a normalizer, exposing in particular the name of the normalizer.
 */
@Incubating
public interface NormalizerDescriptor extends AnalysisDescriptor {
}
