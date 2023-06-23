/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.runtime.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;

public class FromDocumentValueConvertContextImpl implements FromDocumentValueConvertContext {
	private final BackendSessionContext sessionContext;

	public FromDocumentValueConvertContextImpl(BackendSessionContext sessionContext) {
		this.sessionContext = sessionContext;
	}

	@Override
	@Deprecated
	public <T> T extension(
			org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension<
					T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}

	@Override
	public <T> T extension(FromDocumentValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}
}
