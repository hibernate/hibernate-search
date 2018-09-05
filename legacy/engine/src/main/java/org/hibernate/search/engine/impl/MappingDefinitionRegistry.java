/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.search.exception.SearchException;

/**
 * Stores definitions encountered while inspecting the mapping,
 * throwing exceptions when conflicting definitions are registered.
 *
 * @param I the input type of definitions
 * @param O the output of definitions
 *
 * @author Yoann Rodiere
 */
public class MappingDefinitionRegistry<I, O> {

	/**
	 * Constant used as definition point for a global definition
	 * (which, incidentally, can only happen when using the programmatic API).
	 * <p>
	 * In this case no annotated element is available to be used as definition point.
	 */
	private static final String GLOBAL_DEFINITION = "GLOBAL_DEFINITION";

	/**
	 * Map of collected definitions.
	 * The key of the map is the definition name and the value is the definition itself.
	 */
	private final Map<String, O> definitions = new HashMap<String, O>();

	/**
	 * Used to keep track of conflicting definitions.
	 * The key of the map is the definition name the value is a string defining the location of the definition.
	 * <p>
	 * In most cases the fully specified class name together with the annotated element name is used.
	 * See also {@link #GLOBAL_DEFINITION}.
	 */
	private final Map<String, String> definitionPoints = new HashMap<String, String>();

	private final Function<I, O> definitionInterpreter;

	private final Function<String, SearchException> duplicateDefinitionReporter;

	public MappingDefinitionRegistry(Function<I, O> definitionInterpreter, Function<String, SearchException> duplicateDefinitionReporter) {
		super();
		this.definitionInterpreter = definitionInterpreter;
		this.duplicateDefinitionReporter = duplicateDefinitionReporter;
	}

	public void registerGlobal(String name, I definition) {
		register( name, definition, GLOBAL_DEFINITION );
	}

	/**
	 * Add definition.
	 *
	 * @param definition the definition
	 * @param annotatedElement the annotated element it was defined on
	 */
	public void registerFromAnnotation(String name, I definition, XAnnotatedElement annotatedElement) {
		register( name, definition, buildAnnotationDefinitionPoint( annotatedElement ) );
	}

	public Map<String, O> getAll() {
		return Collections.unmodifiableMap( definitions );
	}

	private void register(String name, I definition, String definitionPoint) {
		if ( definitionPoints.containsKey( name ) ) {
			if ( !definitionPoints.get( name ).equals( definitionPoint ) ) {
				throw duplicateDefinitionReporter.apply( name );
			}
		}
		else {
			definitions.put( name, definitionInterpreter.apply( definition ) );
			definitionPoints.put( name, definitionPoint );
		}
	}

	/**
	 * @param annotatedElement an annotated element
	 *
	 * @return a string which identifies the location/point the annotation was placed on. Something of the
	 * form package.[[className].[field|member]]
	 */
	private String buildAnnotationDefinitionPoint(XAnnotatedElement annotatedElement) {
		if ( annotatedElement instanceof XClass ) {
			return ( (XClass) annotatedElement ).getName();
		}
		else if ( annotatedElement instanceof XMember ) {
			XMember member = (XMember) annotatedElement;
			return member.getType().getName() + '.' + member.getName();
		}
		else if ( annotatedElement instanceof XPackage ) {
			return ( (XPackage) annotatedElement ).getName();
		}
		else {
			throw new SearchException( "Unknown XAnnotatedElement: " + annotatedElement );
		}
	}

}
