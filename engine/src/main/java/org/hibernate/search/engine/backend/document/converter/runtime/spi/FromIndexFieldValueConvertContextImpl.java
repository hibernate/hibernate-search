/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.runtime.spi;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class FromIndexFieldValueConvertContextImpl implements FromIndexFieldValueConvertContext {
	private final SessionContextImplementor sessionContext;

	public FromIndexFieldValueConvertContextImpl(SessionContextImplementor sessionContext) {
		this.sessionContext = sessionContext;
	}

	@Override
	public <T> T extension(FromIndexFieldValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}
}
