package org.hibernate.search.build.json;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/** Import names used in generated type and codec expressions, retaining qualification for collisions. */
final class SourceImports {
	private final String packageName;
	private final Set<String> packageTypes = new TreeSet<>();
	private final Set<String> referencedTypes = new TreeSet<>();
	private final Map<String, String> names = new LinkedHashMap<>();
	private Pattern references;

	SourceImports(String packageName, TypeElement root) {
		this.packageName = packageName;
		Element enclosing = root.getEnclosingElement();
		while ( !( enclosing instanceof PackageElement ) ) {
			enclosing = enclosing.getEnclosingElement();
		}
		for ( Element element : enclosing.getEnclosedElements() ) {
			if ( element instanceof TypeElement ) {
				packageTypes.add( element.getSimpleName().toString() );
			}
		}
	}

	void add(String type) {
		referencedTypes.add( type );
		names.putIfAbsent( simpleName( type ), type );
		references = null;
	}

	void add(TypeMirror type) {
		if ( type instanceof DeclaredType declared ) {
			add( ( (TypeElement) declared.asElement() ).getQualifiedName().toString() );
			for ( TypeMirror argument : declared.getTypeArguments() ) {
				add( argument );
			}
		}
	}

	String shorten(String expression) {
		if ( references == null ) {
			// Match complete known type names, with nested types before their enclosing types.
			String alternatives = referencedTypes.stream()
					.sorted( Comparator.comparingInt( String::length ).reversed().thenComparing( Comparator.naturalOrder() ) )
					.map( Pattern::quote ).collect( Collectors.joining( "|" ) );
			references = Pattern.compile( "(?<![\\w$])(?:" + alternatives + ")(?![\\w$])" );
		}
		Matcher matcher = references.matcher( expression );
		StringBuilder result = new StringBuilder();
		while ( matcher.find() ) {
			String type = matcher.group();
			String simple = simpleName( type );
			matcher.appendReplacement( result, Matcher.quoteReplacement( type.equals( names.get( simple ) ) ? simple : type ) );
		}
		matcher.appendTail( result );
		return result.toString();
	}

	Set<String> declarations() {
		Set<String> result = new TreeSet<>();
		for ( String type : names.values() ) {
			String owner = type.substring( 0, type.lastIndexOf( '.' ) );
			if ( !owner.equals( packageName )
					&& ( !owner.equals( "java.lang" ) || packageTypes.contains( simpleName( type ) ) ) ) {
				result.add( type );
			}
		}
		return result;
	}

	private static String simpleName(String type) {
		return type.substring( type.lastIndexOf( '.' ) + 1 );
	}
}
