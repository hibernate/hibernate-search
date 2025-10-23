/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.gson.spi;

import java.util.Set;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Centralizes the configuration of the Gson objects.
 */
public final class GsonProvider {

	/*
	 * https://github.com/google/gson/issues/764 is supposedly fixed.
	 * Hence, we may not need the workaround anymore.
	 * This version will be used in the test so we'll notice if things are still broken, for the main code
	 * the workaround is in place through the GsonProviderHelper.
	 */
	public static GsonProvider create(Supplier<GsonBuilder> builderBaseSupplier, boolean logPrettyPrinting) {
		return create( builderBaseSupplier, logPrettyPrinting, Set.of() );
	}

	public static GsonProvider create(Supplier<GsonBuilder> builderBaseSupplier, boolean logPrettyPrinting,
			Set<TypeToken<?>> typeTokensToInit) {
		return new GsonProvider( builderBaseSupplier, logPrettyPrinting, typeTokensToInit );
	}

	private final Gson gson;

	private final Gson gsonNoSerializeNulls;

	private final JsonLogHelper logHelper;

	private GsonProvider(Supplier<GsonBuilder> builderBaseSupplier, boolean logPrettyPrinting,
			Set<TypeToken<?>> typeTokensToInit) {
		// Null serialization needs to be enabled to index null fields
		gson = builderBaseSupplier.get()
				.serializeNulls()
				.create();
		initializeTypeAdapters( gson, typeTokensToInit );

		gsonNoSerializeNulls = builderBaseSupplier.get()
				.create();
		initializeTypeAdapters( gsonNoSerializeNulls, typeTokensToInit );

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
	private static void initializeTypeAdapters(Gson gson, Set<TypeToken<?>> typeTokensToInit) {
		for ( TypeToken<?> typeToken : typeTokensToInit ) {
			gson.getAdapter( typeToken );
		}
	}

}
