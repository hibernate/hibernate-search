/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.LocalDirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.LocalHeapDirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

public class LuceneBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				BackendFactory.class, LuceneBackendSettings.TYPE_NAME,
				factoryCreationContext -> BeanHolder.of( new LuceneBackendFactory() )
		);
		context.define(
				DirectoryProvider.class, LocalDirectoryProvider.NAME,
				factoryCreationContext -> BeanHolder.of( new LocalDirectoryProvider() )
		);
		context.define(
				DirectoryProvider.class, LocalHeapDirectoryProvider.NAME,
				factoryCreationContext -> BeanHolder.of( new LocalHeapDirectoryProvider() )
		);
	}
}
