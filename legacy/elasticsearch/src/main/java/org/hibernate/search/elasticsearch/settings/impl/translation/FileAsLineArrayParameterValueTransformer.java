/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.hibernate.search.exception.SearchException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

abstract class FileAsLineArrayParameterValueTransformer implements ParameterValueTransformer {

	private static final Pattern FILE_PATH_SEPARATOR_PATTERN = Pattern.compile( "[\\s,]+" );

	private final ResourceLoader resourceLoader;

	public FileAsLineArrayParameterValueTransformer(ResourceLoader resourceLoader) {
		super();
		this.resourceLoader = resourceLoader;
	}

	@Override
	public JsonElement transform(String parameterValue) {
		JsonArray array = new JsonArray();
		for ( String filePath : FILE_PATH_SEPARATOR_PATTERN.split( parameterValue ) ) {
			try ( final InputStream stream = resourceLoader.openResource( filePath ) ) {
				List<String> lines = getLines( stream );
				for ( String line : lines ) {
					array.add( new JsonPrimitive( line ) );
				}
			}
			catch (IOException | SearchException e) {
				throw new SearchException( "Could not parse file: " + parameterValue, e );
			}
		}
		return array;
	}

	protected abstract List<String> getLines(InputStream stream) throws IOException;

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}