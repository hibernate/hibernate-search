/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;

import com.google.gson.JsonElement;

public final class ElasticsearchStandardFieldConverter<F> implements ElasticsearchFieldConverter {

	private final UserIndexFieldConverter<F> userConverter;
	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchStandardFieldConverter(UserIndexFieldConverter<F> userConverter, ElasticsearchFieldCodec<F> codec) {
		this.userConverter = userConverter;
		this.codec = codec;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + userConverter + "]";
	}

	@Override
	public JsonElement convertDslToIndex(Object value, ToIndexFieldValueConvertContext context) {
		F rawValue = userConverter.convertDslToIndex( value, context );
		return codec.encode( rawValue );
	}

	@Override
	public Object convertIndexToProjection(JsonElement element, FromIndexFieldValueConvertContext context) {
		F rawValue = codec.decode( element );
		return userConverter.convertIndexToProjection( rawValue, context );
	}

	@Override
	public boolean isConvertDslToIndexCompatibleWith(ElasticsearchFieldConverter other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldConverter<?> castedOther = (ElasticsearchStandardFieldConverter<?>) other;
		return userConverter.isConvertDslToIndexCompatibleWith( castedOther.userConverter )
				&& codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean isConvertIndexToProjectionCompatibleWith(ElasticsearchFieldConverter other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldConverter<?> castedOther = (ElasticsearchStandardFieldConverter<?>) other;
		return userConverter.isConvertIndexToProjectionCompatibleWith( castedOther.userConverter )
				&& codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean isProjectionCompatibleWith(Class<?> projectionType) {
		return userConverter.isProjectionCompatibleWith( projectionType );
	}
}
