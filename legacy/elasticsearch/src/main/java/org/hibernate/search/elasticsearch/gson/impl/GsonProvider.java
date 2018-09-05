/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import org.hibernate.search.elasticsearch.util.impl.JsonLogHelper;

import com.google.gson.Gson;

/**
 * Centralizes the configuration of the Gson objects.
 *
 * @author Guillaume Smet
 */
public interface GsonProvider {

	Gson getGson();

	/**
	 * @return Same as {@link #getGson()}, but with null serialization turned off.
	 */
	Gson getGsonNoSerializeNulls();

	JsonLogHelper getLogHelper();

}
