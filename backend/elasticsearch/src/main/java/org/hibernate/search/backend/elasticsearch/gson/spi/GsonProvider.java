/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Centralizes the configuration of the Gson objects.
 */
public final class GsonProvider {

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
		return new GsonProvider( builderBaseSupplier, logPrettyPrinting );
	}

	private final Gson gson;

	private final Gson gsonNoSerializeNulls;

	private final JsonLogHelper logHelper;

	private GsonProvider(Supplier<GsonBuilder> builderBaseSupplier, boolean logPrettyPrinting) {
		// Null serialization needs to be enabled to index null fields
		gson = builderBaseSupplier.get()
				.serializeNulls()
				.create();
		initializeTypeAdapters( gson );

		gsonNoSerializeNulls = builderBaseSupplier.get()
				.create();
		initializeTypeAdapters( gsonNoSerializeNulls );

		logHelper = JsonLogHelper.create( builderBaseSupplier.get(), logPrettyPrinting );
	}

	public Gson getGson() {
		return gson;
	}

	/**
	 * @return Same as {@link #getGson()}, but with null serialization turned off.
	 */
	public Gson getGsonNoSerializeNulls() {
		return gsonNoSerializeNulls;
	}

	public JsonLogHelper getLogHelper() {
		return logHelper;
	}

	/*
	 * Workaround for https://github.com/google/gson/issues/764.
	 * We just initialize every adapter known to cause problems before we make the Gson object
	 * available to multiple threads.
	 */
	private static void initializeTypeAdapters(Gson gson) {
		for ( TypeToken<?> typeToken : TYPES_CAUSING_GSON_CONCURRENT_INITIALIZATION_BUG ) {
			gson.getAdapter( typeToken );
		}
	}

}
