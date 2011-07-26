/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.reader.impl;

import java.util.Properties;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class ReaderProviderFactory {

	public static ReaderProvider createReaderProvider(SearchConfiguration cfg, BuildContext context) {
		Properties props = new MaskedProperty( cfg.getProperties(), Environment.READER_PREFIX );
		String impl = props.getProperty( "strategy" );
		ReaderProvider readerProvider;
		if ( StringHelper.isEmpty( impl ) ) {
			//put another one
			readerProvider = new SharingBufferReaderProvider();
		}
		else if ( "not-shared".equalsIgnoreCase( impl ) ) {
			readerProvider = new NotSharedReaderProvider();
		}
		else if ( "shared".equalsIgnoreCase( impl ) ) {
			readerProvider = new SharingBufferReaderProvider();
		}
		else {
			readerProvider = ClassLoaderHelper.instanceFromName(
					ReaderProvider.class, impl,
					ReaderProviderFactory.class, "readerProvider"
			);
		}
		readerProvider.initialize( props, context );
		return readerProvider;
	}
}
