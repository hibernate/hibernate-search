/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.resolver;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

//tag::include[]
public class MyDefaultBridgesConfigurer implements HibernateOrmSearchMappingConfigurer {
	@Override
	public void configure(HibernateOrmMappingConfigurationContext context) {
		context.bridges().exactType( MyCoordinates.class )
				.valueBridge( new MyCoordinatesBridge() ); // <1>

		context.bridges().exactType( MyProductId.class )
				.identifierBridge( new MyProductIdBridge() ); // <2>

		context.bridges().exactType( ISBN.class )
				.valueBinder( new ValueBinder() { // <3>
					@Override
					public void bind(ValueBindingContext<?> context) {
						context.bridge( ISBN.class, new ISBNValueBridge(),
								context.typeFactory().asString().normalizer( "isbn" ) );
					}
				} );
		//end::include[]
		//tag::advanced[]
		context.bridges().subTypesOf( Enum.class ) // <1>
				.valueBinder( new ValueBinder() {
					@Override
					public void bind(ValueBindingContext<?> context) {
						Class<?> enumType = context.bridgedElement().rawType(); // <2>
						doBind( context, enumType );
					}

					private <T> void doBind(ValueBindingContext<?> context, Class<T> enumType) {
						BeanHolder<EnumLabelService> serviceHolder =
								context.beanResolver().resolve( EnumLabelService.class ); // <3>
						context.bridge( enumType, new EnumLabelBridge<>( enumType, serviceHolder ) ); // <4>
					}
				} );
		//end::advanced[]
		//tag::include[]
	}
}
//end::include[]
