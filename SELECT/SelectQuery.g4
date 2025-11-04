/*
 * ANTLR4 grammar for SELECT queries on Jolie trees
 *
 * Usage:
 *   antlr4 SelectQuery.g4
 *   javac SelectQuery*.java
 */

grammar SelectQuery;

// Parser rules
pattern
    : '$' segment* EOF
    ;

segment
    : '..' ID '[*]'             # DescendantArraySegment
    | '..' ID                   # DescendantSegment
    | '.' token                 # DotSegment
    ;

token
    : '*[*]'                    # WildcardArray
    | '*'                       # Wildcard
    | ID '[*]'                  # FieldArray
    | ID                        # Field
    ;

whereClause
    : '.' '=' value             # NodeValue
    | ID 'in' '.'               # FieldExists
    | wherePath '=' value       # PathMatch
    ;

wherePath
    : wherePathSegment+
    ;

wherePathSegment
    : '..' ID                   # WhereDescendantSegment
    | '.' ID                    # WhereDirectSegment
    ;

value
    : ID | NUMBER
    ;

// Lexer rules
ID      : [a-zA-Z_][a-zA-Z0-9_]* ;
NUMBER  : [0-9]+ ;
WS      : [ \t\r\n]+ -> skip ;
