/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.store.LockFactory;

public interface DirectoryCreationContext {

	/**
	 * @return The event context to use for exceptions.
	 */
	EventContext eventContext();

	/**
	 * @return The name of the index in Hibernate Search.
	 */
	String indexName();

	/**
	 * @return The identifier of the index shard, if relevant.
	 */
	Optional<String> shardId();

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	/**
	 * @return A configuration property source, appropriately masked so that the factory
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key "type" is reserved for use by the engine.
	 */
	ConfigurationPropertySource configurationPropertySource();

	Optional<Supplier<LockFactory>> createConfiguredLockFactorySupplier();

}
