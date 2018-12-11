/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.runtime.spi;

import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class FromDocumentFieldValueConvertContextImpl implements FromDocumentFieldValueConvertContext {
	private final SessionContextImplementor sessionContext;

	public FromDocumentFieldValueConvertContextImpl(SessionContextImplementor sessionContext) {
		this.sessionContext = sessionContext;
	}

	@Override
	public <T> T extension(FromDocumentFieldValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}
}
