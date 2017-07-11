/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.spi;

import java.io.IOException;
import java.security.MessageDigest;

/**
 * Interface to be applied on HttpEntity instances to
 * customize the digest encoding:
 * in some cases we can have more efficient code by delegating
 * the computation of digests to the container object.
 *
 * @author Sanne Grinovero (C) 2017 Red Hat Inc.
 */
public interface DigestSelfSigningCapable {

	void fillDigest(MessageDigest dig) throws IOException;

}
