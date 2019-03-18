/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import java.util.function.Function;

import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.util.common.reporting.EventContextElement;

/**
 * Tagging interface for objects used as a key to retrieve mappings at different states:
 * when finalizing the mapping in {@link SearchIntegrationPartialBuildState#finalizeMapping(MappingKey, Function)}
 * or when retrieving the mapping later in {@link SearchIntegration#getMapping(MappingKey)}.
 *
 * @param <PBM> The type of pre-built mappings (see {@link SearchIntegrationPartialBuildState#finalizeMapping(MappingKey, Function)}
 * @param <M> The type of fully-built mappings
 */
public interface MappingKey<PBM, M> extends EventContextElement {

}
