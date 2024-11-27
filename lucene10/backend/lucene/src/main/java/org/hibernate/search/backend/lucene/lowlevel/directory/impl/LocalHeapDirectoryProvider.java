/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;

public class LocalHeapDirectoryProvider implements DirectoryProvider {

	public static final String NAME = "local-heap";

	@Override
	public DirectoryHolder createDirectoryHolder(DirectoryCreationContext context) {
		Supplier<LockFactory> lockFactorySupplier = context.createConfiguredLockFactorySupplier()
				.orElseGet( () -> SingleInstanceLockFactory::new );
		return new LocalHeapDirectoryHolder( lockFactorySupplier.get() );
	}
}
