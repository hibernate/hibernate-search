/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Obtains the target directory of this module.
 *
 * @author Gunnar Morling
 */
public class TargetDirHelper {

	private TargetDirHelper() {
	}

	/**
	 * Returns the target directory of this module.
	 */
	public static Path getTargetDir() {
		URI classesDirUri;
		try {
			classesDirUri = TargetDirHelper.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( e );
		}

		return Paths.get( classesDirUri ).getParent();
	}
}
