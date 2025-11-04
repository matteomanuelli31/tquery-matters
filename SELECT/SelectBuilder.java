import generated.*;
import org.antlr.v4.runtime.*;
import java.util.*;

/**
 * SELECT query builder using ANTLR4 parser.
 * Core: everything is an array (a.b = a.b[0])
 */
public class SelectBuilder {
        private String selectQuery;
        private Value tree;
        private String whereQuery = "";
        private String rootPath = "";

        public SelectBuilder select( String query ) {
                this.selectQuery = query;
                return this;
        }

        public SelectBuilder from( Value tree, String path ) {
                this.tree = tree;
                this.rootPath = path;
                return this;
        }

        public SelectBuilder where( String query ) {
                this.whereQuery = query;
                return this;
        }

        public List<String> exec() {
                Objects.requireNonNull( tree );
                Objects.requireNonNull( selectQuery );

                // Parse the query string into ANTLR parse tree
                final CharStream input = CharStreams.fromString( selectQuery );
                final SelectQueryLexer lexer = new SelectQueryLexer( input );
                final CommonTokenStream tokenStream = new CommonTokenStream( lexer );
                final SelectQueryParser parser = new SelectQueryParser( tokenStream );

                // Get the root of the parse tree (corresponds to 'pattern' grammar rule)
                final SelectQueryParser.PatternContext parseTree = parser.pattern();

                // Visit the parse tree to execute the query
                // visit() dispatches to visitPattern() automatically
                return new QueryVisitor( tree, whereQuery, rootPath ).visit( parseTree );
        }

        private static class QueryVisitor extends SelectQueryBaseVisitor<List<String>> {
                private final Value root;
                private final String whereQuery;
                private final String rootPath;
                private final List<String> results = new ArrayList<>();

                public QueryVisitor( Value root, String whereQuery, String rootPath ) {
                        this.root = root;
                        this.whereQuery = whereQuery;
                        this.rootPath = rootPath;
                }

                @Override
                public List<String> visitPattern( SelectQueryParser.PatternContext ctx ) {
                        final List<SelectQueryParser.SegmentContext> segments = ctx.segment();
                        // $ is mandatory, so always start with rootPath
                        navigate( root, segments, rootPath );
                        return results;
                }

                private void navigate( Value node, List<SelectQueryParser.SegmentContext> segments, String path ) {
                        if( segments.isEmpty() ) return;

                        final SelectQueryParser.SegmentContext currSegment = segments.get( 0 );
                        final List<SelectQueryParser.SegmentContext> otherSegments = segments.subList( 1, segments.size() );

                        switch( currSegment ) {
                                case SelectQueryParser.DescendantArraySegmentContext descendantArray -> {
                                        final String fieldName = descendantArray.ID().getText();
                                        final boolean searchAllArrElems = true;
                                        searchDescendant( node, fieldName, path, otherSegments, searchAllArrElems );
                                }

                                case SelectQueryParser.DescendantSegmentContext descendant -> {
                                        final String fieldName = descendant.ID().getText();
                                        final boolean searchAllArrElems = false;
                                        searchDescendant( node, fieldName, path, otherSegments, searchAllArrElems );
                                }

                                case SelectQueryParser.DotSegmentContext dotSeg -> {
                                        final SelectQueryParser.TokenContext token = dotSeg.token();
                                        switch( token ) {
                                                case SelectQueryParser.WildcardArrayContext wildcardArray -> {
                                                        node.children().forEach( (fieldName, fieldArray) -> {
                                                                final String fieldPath = buildPath( path, fieldName );
                                                                array( fieldArray, fieldPath, otherSegments );
                                                        });
                                                }

                                                case SelectQueryParser.WildcardContext wildcard -> {
                                                        node.children().forEach( (fieldName, fieldArray) -> {
                                                                if( fieldArray.isEmpty() ) return;

                                                                final String fieldPath = buildPath( path, fieldName );
                                                                final Value firstElement = fieldArray.first();
                                                                processElementOrNavigate( firstElement, fieldPath, fieldName, otherSegments );
                                                        });
                                                }

                                                case SelectQueryParser.FieldArrayContext fieldArray -> {
                                                        final String fieldName = fieldArray.ID().getText();
                                                        if( !node.hasChildren( fieldName ) ) return;

                                                        final ValueVector fieldArrayVector = node.getChildren( fieldName );
                                                        final String fieldPath = buildPath( path, fieldName );
                                                        array( fieldArrayVector, fieldPath, otherSegments );
                                                }

                                                case SelectQueryParser.FieldContext field -> {
                                                        final String fieldName = field.ID().getText();
                                                        if( !node.hasChildren( fieldName ) ) return;

                                                        final ValueVector fieldArray = node.getChildren( fieldName );
                                                        if( fieldArray.isEmpty() ) return;

                                                        final Value firstElement = fieldArray.first();
                                                        final String fieldPath = buildPath( path, fieldName );
                                                        processElementOrNavigate( firstElement, fieldPath, fieldName, otherSegments );
                                                }

                                                default -> throw new IllegalStateException( "Unexpected token type: " + token.getClass() );
                                        }
                                }

                                default -> throw new IllegalStateException( "Unexpected segment type: " + currSegment.getClass() );
                        }
                }

                private void searchDescendant( Value node, String fieldName, String path,
                                List<SelectQueryParser.SegmentContext> otherSegments, boolean searchAllArrElems ) {

                        if( node.hasChildren( fieldName ) ) {
                                final ValueVector fieldArray = node.getChildren( fieldName );
                                final String fieldPath = buildPath( path, fieldName );
                                final boolean hasMoreSegments = !otherSegments.isEmpty();

                                if( searchAllArrElems || hasMoreSegments ) {
                                        array( fieldArray, fieldPath, otherSegments );
                                } else {
                                        if( !fieldArray.isEmpty() ) {
                                                final Value firstElement = fieldArray.first();
                                                testAndAddResult( firstElement, fieldPath, fieldName );
                                        }
                                }
                        }

                        node.children().forEach( (childName, childArray) -> {
                                if( !childArray.isEmpty() ) {
                                        final Value firstChild = childArray.first();
                                        final String childPath = buildPath( path, childName );
                                        searchDescendant( firstChild, fieldName, childPath, otherSegments, searchAllArrElems );
                                }
                        });
                }

                private void array( ValueVector arr, String arrayPath, List<SelectQueryParser.SegmentContext> otherSegments ) {
                        for( int i = 0; i < arr.size(); i++ ) {
                                final Value element = arr.get( i );
                                final String elementPath = String.format( "%s[%d]", arrayPath, i );
                                processElementOrNavigate( element, elementPath, "", otherSegments );
                        }
                }

                // ========== HELPERS ==========

                private void processElementOrNavigate( Value element, String elementPath, String fieldName,
                                List<SelectQueryParser.SegmentContext> otherSegments ) {

                        final boolean isLastToken = otherSegments.isEmpty();

                        if( isLastToken ) {
                                testAndAddResult( element, elementPath, fieldName );
                        } else {
                                navigate( element, otherSegments, elementPath );
                        }
                }

                private void testAndAddResult( Value element, String elementPath, String fieldName ) {
                        final boolean matches = test( element, fieldName );
                        if( matches ) {
                                results.add( elementPath );
                        }
                }

                private String buildPath( String currentPath, String childName ) {
                        return currentPath.isEmpty() ? childName : currentPath + "." + childName;
                }

                private boolean eq( Value value, String expected ) {
                        try {
                                if( value.isInt() ) {
                                        final int actualValue = value.intValue();
                                        final int expectedValue = Integer.parseInt( expected );
                                        return actualValue == expectedValue;
                                }
                                if( value.isString() ) {
                                        final String actualValue = value.strValue();
                                        return actualValue.equals( expected );
                                }
                                if( value.isBool() ) {
                                        final boolean actualValue = value.boolValue();
                                        final boolean expectedValue = Boolean.parseBoolean( expected );
                                        return actualValue == expectedValue;
                                }
                        } catch( NumberFormatException e ) {
                                // Type mismatch
                        }
                        return false;
                }

                // ========== WHERE ==========

                private boolean test( Value node, String nodeName ) {
                        final String trimmedWhereQuery = whereQuery.trim();
                        if( trimmedWhereQuery.isEmpty() ) return true;

                        if( trimmedWhereQuery.startsWith( ". = " ) ) {
                                final String expectedValue = trimmedWhereQuery.substring( 4 ).trim();
                                return node.isDefined() && eq( node, expectedValue );
                        }

                        if( trimmedWhereQuery.endsWith( " in ." ) ) {
                                final String fieldName = trimmedWhereQuery.replace( " in .", "" ).trim();
                                return node.hasChildren( fieldName );
                        }

                        if( trimmedWhereQuery.startsWith( ".." ) ) {
                                final String remainder = trimmedWhereQuery.substring( 2 );
                                final String[] parts = remainder.split( "=", 2 );
                                final String fieldPattern = parts[0].trim();
                                final String expectedValue = parts[1].trim();

                                // Check if field pattern includes [*] for array wildcard
                                final boolean searchAllElements = fieldPattern.endsWith( "[*]" );
                                final String fieldName = searchAllElements ?
                                        fieldPattern.substring( 0, fieldPattern.length() - 3 ) :
                                        fieldPattern;

                                final boolean isDirectMatch = nodeName.equals( fieldName ) && node.isDefined() && eq( node, expectedValue );
                                final boolean isDescendantMatch = desc( node, fieldName, expectedValue, searchAllElements );

                                return isDirectMatch || isDescendantMatch;
                        }

                        if( trimmedWhereQuery.startsWith( "." ) ) {
                                final String remainder = trimmedWhereQuery.substring( 1 );
                                final String[] parts = remainder.split( "=", 2 );
                                final String fieldPath = parts[0].trim();
                                final String expectedValue = parts[1].trim();

                                // Navigate through path like "b.c" or just "b"
                                final String[] pathSegments = fieldPath.split( "\\." );
                                Value current = node;

                                for( String segment : pathSegments ) {
                                        if( !current.hasChildren( segment ) ) return false;

                                        final ValueVector fieldArray = current.getChildren( segment );
                                        if( fieldArray.isEmpty() ) return false;

                                        current = fieldArray.first();
                                }

                                return current.isDefined() && eq( current, expectedValue );
                        }

                        return true;
                }

                private boolean desc( Value node, String fieldName, String expectedValue, boolean searchAllElements ) {
                        if( node.hasChildren( fieldName ) ) {
                                final ValueVector fieldArray = node.getChildren( fieldName );
                                if( searchAllElements ) {
                                        // Check all array elements
                                        for( int i = 0; i < fieldArray.size(); i++ ) {
                                                final Value element = fieldArray.get( i );
                                                if( element.isDefined() && eq( element, expectedValue ) ) {
                                                        return true;
                                                }
                                        }
                                } else {
                                        // Check only first element (field[0])
                                        if( !fieldArray.isEmpty() ) {
                                                final Value element = fieldArray.first();
                                                return element.isDefined() && eq( element, expectedValue );
                                        }
                                }
                        }

                        for( ValueVector childArray : node.children().values() ) {
                                for( int i = 0; i < childArray.size(); i++ ) {
                                        final Value element = childArray.get( i );
                                        final boolean descendantMatches = desc( element, fieldName, expectedValue, searchAllElements );
                                        if( descendantMatches ) {
                                                return true;
                                        }
                                }
                        }

                        return false;
                }
        }
}
