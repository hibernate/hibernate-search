/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.LockingStrategyName;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;

/**
 * The implementation of {@link DirectoryCreationContext}.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class DirectoryCreationContextImpl implements DirectoryCreationContext {

	private static final OptionalConfigurationProperty<LockingStrategyName> LOCKING_STRATEGY =
			ConfigurationProperty.forKey( LuceneIndexSettings.DirectoryRadicals.LOCKING_STRATEGY )
					.as( LockingStrategyName.class, LockingStrategyName::of )
					.build();

	private final EventContext eventContext;
	private final String indexName;
	private final Optional<String> shardId;
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;

	public DirectoryCreationContextImpl(EventContext eventContext, String indexName, Optional<String> shardId,
			BeanResolver beanResolver, ConfigurationPropertySource configurationPropertySource) {
		this.eventContext = eventContext;
		this.indexName = indexName;
		this.shardId = shardId;
		this.beanResolver = beanResolver;
		this.configurationPropertySource = configurationPropertySource;
	}

	@Override
	public EventContext eventContext() {
		return eventContext;
	}

	@Override
	public String indexName() {
		return indexName;
	}

	@Override
	public Optional<String> shardId() {
		return shardId;
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return configurationPropertySource;
	}

	@Override
	public Optional<Supplier<LockFactory>> createConfiguredLockFactorySupplier() {
		// TODO HSEARCH-3635 Restore support for configuring a custom LockFactory
		return LOCKING_STRATEGY.get( configurationPropertySource )
				.map( DirectoryCreationContextImpl::createLockFactorySupplier );
	}

	private static Supplier<LockFactory> createLockFactorySupplier(LockingStrategyName name) {
		switch ( name ) {
			case SIMPLE_FILESYSTEM:
				return () -> SimpleFSLockFactory.INSTANCE;
			case NATIVE_FILESYSTEM:
				return () -> NativeFSLockFactory.INSTANCE;
			case SINGLE_INSTANCE:
				return () -> new SingleInstanceLockFactory();
			case NONE:
				return () -> NoLockFactory.INSTANCE;
		}
		throw new AssertionFailure( "Unexpected name: " + name );
	}

}
