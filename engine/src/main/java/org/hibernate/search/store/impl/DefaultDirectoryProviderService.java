/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.util.Locale;

import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.spi.BaseDirectoryProviderService;

/**
 * Default {@link org.hibernate.search.store.impl.DefaultDirectoryProviderService}
 *
 * @author gustavonalle
 */
public class DefaultDirectoryProviderService extends BaseDirectoryProviderService {

	@Override
	public Class<? extends DirectoryProvider> getDefault() {
		return FSDirectoryProvider.class;
	}

	@Override
	public String toFullyQualifiedClassName(String name) {
		String maybeShortCut = name.toLowerCase( Locale.ROOT );
		if ( defaultProviderClasses.containsKey( maybeShortCut ) ) {
			return defaultProviderClasses.get( maybeShortCut );
		}
		return name;
	}

}
