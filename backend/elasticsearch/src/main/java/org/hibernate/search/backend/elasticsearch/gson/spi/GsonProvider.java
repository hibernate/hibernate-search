/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Centralizes the configuration of the Gson objects.
 */
public final class GsonProvider {

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

		gsonNoSerializeNulls = builderBaseSupplier.get()
				.create();

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

}
