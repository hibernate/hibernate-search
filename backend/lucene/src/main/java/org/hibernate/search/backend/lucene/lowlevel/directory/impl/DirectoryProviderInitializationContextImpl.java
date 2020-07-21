/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.LockingStrategyName;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProviderInitializationContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;

public class DirectoryProviderInitializationContextImpl implements DirectoryProviderInitializationContext {

	private static final ConfigurationProperty<BeanReference<? extends DirectoryProvider>> TYPE =
			ConfigurationProperty.forKey( LuceneIndexSettings.DirectoryRadicals.TYPE )
					.asBeanReference( DirectoryProvider.class )
					.withDefault( BeanReference.of( DirectoryProvider.class, LuceneIndexSettings.Defaults.DIRECTORY_TYPE ) )
					.build();

	private static final OptionalConfigurationProperty<LockingStrategyName> LOCKING_STRATEGY =
			ConfigurationProperty.forKey( LuceneIndexSettings.DirectoryRadicals.LOCKING_STRATEGY )
					.as( LockingStrategyName.class, LockingStrategyName::of )
					.build();

	private final EventContext eventContext;
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;

	public DirectoryProviderInitializationContextImpl(EventContext eventContext,
			BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource) {
		this.eventContext = eventContext;
		this.beanResolver = beanResolver;
		this.configurationPropertySource = configurationPropertySource;
	}

	@Override
	public EventContext eventContext() {
		return eventContext;
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
				.map( DirectoryProviderInitializationContextImpl::createLockFactorySupplier );
	}

	public BeanHolder<? extends DirectoryProvider> createDirectoryProvider() {
		BeanHolder<? extends DirectoryProvider> directoryProviderHolder =
				TYPE.getAndTransform(
						configurationPropertySource,
						beanResolver::resolve
				);
		try {
			directoryProviderHolder.get().initialize( this );
			return directoryProviderHolder;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), directoryProviderHolder )
					.push( directoryProviderHolder );
			throw e;
		}
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
