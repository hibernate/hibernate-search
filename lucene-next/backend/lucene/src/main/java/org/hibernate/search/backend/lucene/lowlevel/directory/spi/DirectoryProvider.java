/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

public interface DirectoryProvider {

	/**
	 * Creates a {@link DirectoryHolder} for a given name,
	 * but do <strong>not</strong> allocate resources yet
	 * (wait until {@link DirectoryHolder#start()} is called).
	 * <p>
	 * The provided index names are raw and do not take into account the limitations of the internal representation
	 * of indexes. If some characters cannot be used in a given {@link DirectoryProvider},
	 * this provider is expected to escape characters as necessary using an encoding scheme assigning
	 * a unique representation to each index name,
	 * so as to avoid two index names to be encoded into identical internal representations.
	 * Lower-casing the index name, for example, is not an acceptable encoding scheme,
	 * as two index names differing only in case could end up using the same directory.
	 *
	 * @param context The creation context, giving access to configuration and environment.
	 * @return The directory holder to use for that index name
	 */
	DirectoryHolder createDirectoryHolder(DirectoryCreationContext context);

}
