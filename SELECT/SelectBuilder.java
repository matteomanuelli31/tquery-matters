import java.util.*;

/**
 * SELECT query builder for Jolie trees.
 * Core: everything is an array (a.b = a.b[0])
 */
public class SelectBuilder {
        private String pattern;
        private Value tree;
        private String where = "";
        private List<String> results = new ArrayList<>();

        public SelectBuilder select( String pattern ) {
                this.pattern = pattern;
                return this;
        }

        public SelectBuilder from( Value tree ) {
                this.tree = tree;
                return this;
        }

        public SelectBuilder where( String condition ) {
                this.where = condition;
                return this;
        }

        public List<String> exec() {
                Objects.requireNonNull( tree );
                Objects.requireNonNull( pattern );

                final String[] tokens = tokenize( pattern );
                navigate( tree, tokens, "" );
                return results;
        }

        // Tokenize pattern preserving ".." as prefix
        private String[] tokenize( String pattern ) {
                // Split on single dots but preserve ".." as part of the token
                return pattern.split( "(?<!\\.)\\." + "(?!\\.)" );
        }

        // ========== NAVIGATE ==========

        private void navigate( Value node, String[] tokens, String path ) {
                if( tokens.length == 0 ) return;

                final String currToken = tokens[0];
                final String[] otherTokens = Arrays.copyOfRange( tokens, 1, tokens.length );

                if( "*[*]".equals( currToken ) ) {
                        node.children().forEach( (fieldName, fieldArray) -> {
                                final String fieldPath = path( path, fieldName );
                                array( fieldArray, fieldPath, otherTokens );
                        });

                } else if( "*".equals( currToken ) ) {
                        node.children().forEach( (fieldName, fieldArray) -> {
                                final String fieldPath = path( path, fieldName );

                                if( otherTokens.length == 0 ) {
                                        if( !fieldArray.isEmpty() && test( fieldArray.first(), fieldName ) ) {
                                                results.add( fieldPath );
                                        }
                                } else {
                                        if( !fieldArray.isEmpty() ) {
                                                // a.b = a.b[0]
                                                navigate( fieldArray.first(), otherTokens, fieldPath );
                                        }
                                }
                        });

                } else if( currToken.startsWith( ".." ) ) {
                        // Descendant pattern: ..field finds all fields named "field" at any depth
                        final String fieldName = currToken.substring( 2 );
                        searchDescendant( node, fieldName, path, otherTokens );

                } else if( currToken.endsWith( "[*]" ) ) {
                        final String fieldName = currToken.replace( "[*]", "" );
                        if( !node.hasChildren( fieldName ) ) return;

                        final ValueVector fieldArray = node.getChildren( fieldName );
                        final String fieldPath = path( path, fieldName );
                        array( fieldArray, fieldPath, otherTokens );

                } else {
                        final String fieldName = currToken;
                        if( !node.hasChildren( fieldName ) ) return;

                        final ValueVector fieldArray = node.getChildren( fieldName );
                        if( fieldArray.isEmpty() ) return;

                        final Value firstElement = fieldArray.first();
                        final String fieldPath = path( path, fieldName );
                        navigate( firstElement, otherTokens, fieldPath );
                }
        }

        private void searchDescendant( Value node, String fieldName, String path, String[] otherTokens ) {
                if( node.hasChildren( fieldName ) ) {
                        final ValueVector fieldArray = node.getChildren( fieldName );
                        final String fieldPath = path( path, fieldName );

                        if( otherTokens.length == 0 ) {
                                if( !fieldArray.isEmpty() && test( fieldArray.first(), fieldName ) ) {
                                        results.add( fieldPath );
                                }
                        } else {
                                array( fieldArray, fieldPath, otherTokens );
                        }
                }

                node.children().forEach( (childName, childArray) -> {
                        if( !childArray.isEmpty() ) {
                                final Value firstChild = childArray.first();
                                final String childPath = path( path, childName );
                                searchDescendant( firstChild, fieldName, childPath, otherTokens );
                        }
                });
        }

        private void array( ValueVector arr, String arrayPath, String[] otherTokens ) {
                for( int i = 0; i < arr.size(); i++ ) {
                        final Value element = arr.get( i );
                        final String elementPath = String.format( "%s[%d]", arrayPath, i );

                        if( otherTokens.length == 0 ) {
                                // TODO a.b[n] in select query
                                if( test( element, "" ) ) {
                                        results.add( elementPath );
                                }
                        } else {
                                navigate( element, otherTokens, elementPath );
                        }
                }
        }

        // ========== WHERE ==========

        private boolean test( Value node, String nodeName ) {
                final String condition = where.trim();
                if( condition.isEmpty() ) return true;

                if( condition.startsWith( ". = " ) ) {
                        final String expected = condition.replaceFirst( "\\. = ", "" ).trim();
                        return node.isDefined() && eq( node, expected );
                }

                if( condition.endsWith( " in ." ) ) {
                        final String fieldName = condition.replace( " in .", "" ).trim();
                        return node.hasChildren( fieldName );
                }

                if( condition.startsWith( ".." ) ) {
                        final String remainder = condition.replaceFirst( "\\.\\.", "" );
                        final String[] parts = remainder.split( "=", 2 );
                        final String fieldName = parts[0].trim();
                        final String expectedValue = parts[1].trim();
                        return nodeName.equals( fieldName ) && node.isDefined() && eq( node, expectedValue )
                                || desc( node, fieldName, expectedValue );
                }

                if( condition.startsWith( "." ) ) {
                        final String remainder = condition.replaceFirst( "\\.", "" );
                        final String[] parts = remainder.split( "=", 2 );
                        final String fieldName = parts[0].trim();
                        final String expectedValue = parts[1].trim();

                        if( !node.hasChildren( fieldName ) ) return false;

                        final ValueVector fieldArray = node.getChildren( fieldName );
                        if( fieldArray.isEmpty() ) return false;

                        final Value child = fieldArray.first();
                        return child.isDefined() && eq( child, expectedValue );
                }

                return true;
        }

        private boolean desc( Value node, String fieldName, String expectedValue ) {
                if( node.hasChildren( fieldName ) ) {
                        final ValueVector fieldArray = node.getChildren( fieldName );
                        for( int i = 0; i < fieldArray.size(); i++ ) {
                                final Value element = fieldArray.get( i );
                                if( element.isDefined() && eq( element, expectedValue ) ) {
                                        return true;
                                }
                        }
                }

                for( ValueVector childArray : node.children().values() ) {
                        for( int i = 0; i < childArray.size(); i++ ) {
                                final Value element = childArray.get( i );
                                if( desc( element, fieldName, expectedValue ) ) {
                                        return true;
                                }
                        }
                }

                return false;
        }

        // ========== HELPERS ==========

        private String path( String current, String child ) {
                return current.isEmpty() ? child : current + "." + child;
        }

        private boolean eq( Value value, String expected ) {
                try {
                        if( value.isInt() ) {
                                final int actual = value.intValue();
                                final int expectedInt = Integer.parseInt( expected );
                                return actual == expectedInt;
                        }
                        if( value.isString() ) {
                                final String actual = value.strValue();
                                return actual.equals( expected );
                        }
                        if( value.isBool() ) {
                                final boolean actual = value.boolValue();
                                final boolean expectedBool = Boolean.parseBoolean( expected );
                                return actual == expectedBool;
                        }
                } catch( NumberFormatException e ) {
                        // Type mismatch
                }
                return false;
        }
}
