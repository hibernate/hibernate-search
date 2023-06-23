/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.mapper.orm.mapping.context.HibernateOrmMappingContext;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.SessionBasedBridgeOperationContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ValueBridgeToIndexedValueContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class HibernateOrmExtensionTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private HibernateOrmMappingContextMock mappingContext;
	@Mock
	private HibernateOrmSessionContextMock sessionContext;

	@Test
	public void identifierBridge() {
		IdentifierBridgeToDocumentIdentifierContext toDocumentContext =
				new IdentifierBridgeToDocumentIdentifierContextImpl( mappingContext );
		assertThat( toDocumentContext.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );

		IdentifierBridgeFromDocumentIdentifierContext fromDocumentContext =
				new SessionBasedBridgeOperationContext( sessionContext );
		assertThat( fromDocumentContext.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	@Test
	public void routingBridge() {
		RoutingBridgeRouteContext context = new SessionBasedBridgeOperationContext( sessionContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	@Test
	public void typeBridge() {
		TypeBridgeWriteContext context = new SessionBasedBridgeOperationContext( sessionContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	@Test
	public void propertyBridge() {
		PropertyBridgeWriteContext context = new SessionBasedBridgeOperationContext( sessionContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	@Test
	public void valueBridge() {
		ValueBridgeToIndexedValueContext toIndexedValueContext = new ValueBridgeToIndexedValueContextImpl( mappingContext );
		assertThat( toIndexedValueContext.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );

		ValueBridgeFromIndexedValueContext fromIndexedValueContext = new SessionBasedBridgeOperationContext( sessionContext );
		assertThat( fromIndexedValueContext.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void toDocumentFieldValueConverter() {
		org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext context =
				new ToDocumentValueConvertContextImpl( mappingContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void toDocumentValueConverter() {
		ToDocumentValueConvertContext context = new ToDocumentValueConvertContextImpl( mappingContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( mappingContext );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void fromDocumentFieldValueConverter() {
		org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext context =
				new FromDocumentValueConvertContextImpl( sessionContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void fromDocumentValueConverter() {
		FromDocumentValueConvertContext context = new FromDocumentValueConvertContextImpl( sessionContext );
		assertThat( context.extension( HibernateOrmExtension.get() ) ).isSameAs( sessionContext );
	}

	private interface HibernateOrmMappingContextMock
			extends HibernateOrmMappingContext, BridgeMappingContext, BackendMappingContext {
	}

	private interface HibernateOrmSessionContextMock
			extends HibernateOrmSessionContext, BridgeSessionContext, BackendSessionContext {

		@Override
		HibernateOrmMappingContextMock mappingContext();

	}
}
