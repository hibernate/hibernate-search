/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.runtime;

import java.util.Optional;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

/**
 * An extension to {@link FromDocumentFieldValueConvertContext}, allowing to access non-standard context
 * specific to a given mapper.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended contexts.
 *
 * @see FromDocumentFieldValueConvertContext#extension(FromDocumentFieldValueConvertContextExtension)
 * @deprecated Use {@link org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter}
 * with {@link FromDocumentValueConvertContext} and {@link FromDocumentValueConvertContextExtension} instead.
 */
@Deprecated
public interface FromDocumentFieldValueConvertContextExtension<T> extends FromDocumentValueConvertContextExtension<T> {

	@Override
	default Optional<T> extendOptional(FromDocumentValueConvertContext original, BackendSessionContext sessionContext) {
		return extendOptional( (FromDocumentFieldValueConvertContext) original, sessionContext );
	}

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link FromDocumentFieldValueConvertContext}.
	 * @param sessionContext A {@link BackendSessionContext}.
	 * @return An optional containing the extended context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(FromDocumentFieldValueConvertContext original, BackendSessionContext sessionContext);

}
