/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.impl;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.index.impl.ExplicitShardingStrategy;
import org.hibernate.search.backend.lucene.index.impl.HashShardingStrategy;
import org.hibernate.search.backend.lucene.index.impl.NoShardingStrategy;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategy;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.LocalFileSystemDirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.LocalHeapDirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.resources.impl.DefaultLuceneWorkExecutorProvider;
import org.hibernate.search.backend.lucene.work.spi.LuceneWorkExecutorProvider;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

public class LuceneBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				BackendFactory.class, LuceneBackendSettings.TYPE_NAME,
				beanResolver -> BeanHolder.of( new LuceneBackendFactory() )
		);
		context.define(
				DirectoryProvider.class, LocalFileSystemDirectoryProvider.NAME,
				beanResolver -> BeanHolder.of( new LocalFileSystemDirectoryProvider() )
		);
		context.define(
				DirectoryProvider.class, LocalHeapDirectoryProvider.NAME,
				beanResolver -> BeanHolder.of( new LocalHeapDirectoryProvider() )
		);
		context.define(
				ShardingStrategy.class, NoShardingStrategy.NAME,
				beanResolver -> BeanHolder.of( new NoShardingStrategy() )
		);
		context.define(
				ShardingStrategy.class, HashShardingStrategy.NAME,
				beanResolver -> BeanHolder.of( new HashShardingStrategy() )
		);
		context.define(
				ShardingStrategy.class, ExplicitShardingStrategy.NAME,
				beanResolver -> BeanHolder.of( new ExplicitShardingStrategy() )
		);
		context.define(
				LuceneWorkExecutorProvider.class, DefaultLuceneWorkExecutorProvider.DEFAULT_BEAN_NAME,
				beanResolver -> BeanHolder.of( new DefaultLuceneWorkExecutorProvider() )
		);
	}
}
