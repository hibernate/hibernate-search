/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.IOException;

/**
 * In some cases we can have more efficient code by delegating
 * the computation of the Sha256 to the container object.
 *
 * @author Sanne Grinovero (C) 2017 Red Hat Inc.
 */
public interface DigestSelfSigningCapable {

	/**
	 * Sign the content using an equivalent strategy to
	 * {@link org.apache.commons.codec.digest.DigestUtils.sha256(InputStream)}
	 * @return the computed digest
	 * @throws IOException
	 */
	byte[] getSha256DigestSignature() throws IOException;

}
