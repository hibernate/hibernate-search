/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Centralizes the configuration of the Gson objects.
 */
public final class GsonProviderHelper {

	private GsonProviderHelper() {
	}

	/*
	 * See https://github.com/google/gson/issues/764.
	 *
	 * Only composite type adapters (referring to another type adapter)
	 * should be affected by this bug, so we'll list the corresponding types here.
	 * Maybe it's even narrower, and only type adapters that indirectly refer to themselves are affected,
	 * but I'm not entirely sure about that.
	 *
	 * Note we only need to list "root" types:
	 * all types they refer to will also have their type adapter initialized.
	 */
	private static final Set<TypeToken<?>> TYPES_CAUSING_GSON_CONCURRENT_INITIALIZATION_BUG =
			CollectionHelper.asImmutableSet(
					TypeToken.get( IndexSettings.class ),
					TypeToken.get( RootTypeMapping.class )
			);

	public static GsonProvider create(Supplier<GsonBuilder> builderBaseSupplier, boolean logPrettyPrinting) {
		return GsonProvider.create( builderBaseSupplier, logPrettyPrinting, TYPES_CAUSING_GSON_CONCURRENT_INITIALIZATION_BUG );
	}

}
