/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.common.jar.impl.JandexUtils.readOrBuildIndex;
import static org.hibernate.search.util.common.jar.impl.JarUtils.codeSourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.jar.impl.JandexUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import org.apache.avro.specific.AvroGenerated;

/**
 * Tests that hardcoded lists of classes stay up-to-date.
 */
public class HibernateOrmMapperOutboxPollingClassesTest {

	private static Index outboxPollingIndex;

	@BeforeClass
	public static void index() {
		outboxPollingIndex = readOrBuildIndex(
				codeSourceLocation( HibernateOrmMapperOutboxPollingSettings.class )
						.orElseThrow( () -> new AssertionFailure(
								"Could not find hibernate-search-mapper-orm-outbox-polling JAR?" ) )
		);
	}

	@Test
	public void testNoMissingAvroGeneratedClass() {
		Set<String> annotatedClasses = new HashSet<>();
		for ( AnnotationInstance annotationInstance : outboxPollingIndex
				.getAnnotations( DotName.createSimple( AvroGenerated.class.getName() ) ) ) {
			DotName className = JandexUtils.extractDeclaringClass( annotationInstance.target() ).name();
			annotatedClasses.add( className.toString() );
		}

		assertThat( annotatedClasses ).isNotEmpty();
		assertThat( HibernateOrmMapperOutboxPollingClasses.avroTypes() )
				.containsExactlyInAnyOrderElementsOf( annotatedClasses );
	}

	@Test
	public void testNoMissingJpaModelClass() {
		Set<DotName> modelClasses = collectModelClassesRecursively( outboxPollingIndex, new HashSet<>( Arrays.asList(
				DotName.createSimple( OutboxEvent.class.getName() ),
				DotName.createSimple( Agent.class.getName() )
		) ) );

		Set<String> modelClassNames = modelClasses.stream().map( DotName::toString ).collect( Collectors.toSet() );

		// Despite being referenced from entities, these types are not included in the JPA model.
		modelClassNames.removeAll( Arrays.asList(
				"org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference",
				"org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventReference",
				"org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor"
		) );

		assertThat( modelClassNames ).isNotEmpty();
		assertThat( HibernateOrmMapperOutboxPollingClasses.hibernateOrmTypes() )
				.containsExactlyInAnyOrderElementsOf( modelClassNames );
	}

	private static Set<DotName> collectModelClassesRecursively(Index index, Set<DotName> initialClasses) {
		Set<DotName> classes = new HashSet<>();
		for ( DotName initialClass : initialClasses ) {
			collectModelClassesRecursively( index, initialClass, classes );
		}
		return classes;
	}

	private static void collectModelClassesRecursively(Index index, DotName className, Set<DotName> classes) {
		if ( className.toString().startsWith( "java." ) ) {
			return;
		}
		if ( !classes.add( className ) ) {
			return;
		}
		ClassInfo clazz = index.getClassByName( className );
		collectModelClassesRecursively( index, clazz.superName(), classes );
		for ( DotName interfaceName : clazz.interfaceNames() ) {
			collectModelClassesRecursively( index, interfaceName, classes );
		}
		for ( FieldInfo field : clazz.fields() ) {
			collectModelClassesRecursively( index, field.type(), classes );
		}
		for ( FieldInfo field : clazz.fields() ) {
			collectModelClassesRecursively( index, field.type(), classes );
		}
		for ( MethodInfo methodInfo : clazz.methods() ) {
			if ( !methodInfo.parameters().isEmpty() ) {
				// Definitely not a getter, just skip.
				continue;
			}
			collectModelClassesRecursively( index, methodInfo.returnType(), classes );
		}
	}

	private static void collectModelClassesRecursively(Index index, Type type, Set<DotName> classes) {
		switch ( type.kind() ) {
			case CLASS:
				collectModelClassesRecursively( index, type.name(), classes );
				break;
			case ARRAY:
				collectModelClassesRecursively( index, type.asArrayType().constituent(), classes );
				break;
			case TYPE_VARIABLE:
				for ( Type bound : type.asTypeVariable().bounds() ) {
					collectModelClassesRecursively( index, bound, classes );
				}
				break;
			case WILDCARD_TYPE:
				collectModelClassesRecursively( index, type.asWildcardType().extendsBound(), classes );
				collectModelClassesRecursively( index, type.asWildcardType().superBound(), classes );
				break;
			case PARAMETERIZED_TYPE:
				collectModelClassesRecursively( index, type.name(), classes );
				for ( Type argument : type.asParameterizedType().arguments() ) {
					collectModelClassesRecursively( index, argument, classes );
				}
				break;
			case PRIMITIVE:
			case VOID:
			case UNRESOLVED_TYPE_VARIABLE:
				// Ignore
				break;
		}
	}
}
