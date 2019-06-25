/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;

import java.util.function.Function;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;

public interface SearchIntegrationPartialBuildState {

	/**
	 * Close the resources held by this object (backends, index managers, ...).
	 * <p>
	 * To be called in the event of a failure that will prevent {@link #finalizeIntegration(ConfigurationPropertySource)} to be called.
	 */
	void closeOnFailure();

	/**
	 * Finalize the building of a mapping.
	 * @param mappingKey The mapping key allowing to retrieve the pre-built mapping.
	 * @param director The object responsible for turning a pre-built mapping into a fully-built mapping (it may hold some additional context).
	 * @param <PBM> The type of the partially-built mapping.
	 * @param <M> The type of the fully-built mapping.
	 * @return The fully-built mapping.
	 */
	<PBM, M> M finalizeMapping(MappingKey<PBM, M> mappingKey, Function<PBM, MappingImplementor<M>> director);

	/**
	 * Finalize the building of the integration.
	 * @param configurationPropertySource The configuration property source,
	 * which may hold additional configuration compared to the one passed to
	 * {@link SearchIntegration#builder(ConfigurationPropertySource)}.
	 * @return The fully-built integration.
	 */
	SearchIntegration finalizeIntegration(ConfigurationPropertySource configurationPropertySource);

}
