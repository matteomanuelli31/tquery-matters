import java.util.List;

public class SelectSimplePathTest {
        public static void main( String[] args ) {
                // Create structure:
                // root.a.b.c = 5
                // root.a.b.d = 10
                // root.a.e = 5

                Value a = Value.create();
                Value b = a.getChildren( "b" ).first();
                b.getChildren( "c" ).first().setValue( 5 );
                b.getChildren( "d" ).first().setValue( 10 );
                a.getChildren( "e" ).first().setValue( 5 );

                System.out.println( "Structure created:" );
                System.out.println( "  a.b.c = 5" );
                System.out.println( "  a.b.d = 10" );
                System.out.println( "  a.e = 5" );

                // Test 1: SELECT $.b.c FROM a WHERE . = 5
                System.out.println( "\nTest 1: SELECT $.b.c FROM a WHERE . = 5" );
                List<String> results1 = new SelectBuilder()
                        .select( "$.b.c" )
                        .from( a, "a" )
                        .where( ". = 5" )
                        .exec();
                System.out.println( "Result: " + results1 );
                System.out.println( "Expected: [a.b.c]" );

                if( results1.size() == 1 && results1.contains( "a.b.c" ) ) {
                        System.out.println( "✓ Test 1 PASSED" );
                } else {
                        System.out.println( "✗ Test 1 FAILED" );
                        System.exit( 1 );
                }

                // Test 2: SELECT $.b.d FROM a WHERE . = 5
                System.out.println( "\nTest 2: SELECT $.b.d FROM a WHERE . = 5" );
                List<String> results2 = new SelectBuilder()
                        .select( "$.b.d" )
                        .from( a, "a" )
                        .where( ". = 5" )
                        .exec();
                System.out.println( "Result: " + results2 );
                System.out.println( "Expected: [] (a.b.d = 10, not 5)" );

                if( results2.isEmpty() ) {
                        System.out.println( "✓ Test 2 PASSED" );
                } else {
                        System.out.println( "✗ Test 2 FAILED" );
                        System.exit( 1 );
                }

                // Test 3: SELECT $.e FROM a WHERE . = 5
                System.out.println( "\nTest 3: SELECT $.e FROM a WHERE . = 5" );
                List<String> results3 = new SelectBuilder()
                        .select( "$.e" )
                        .from( a, "a" )
                        .where( ". = 5" )
                        .exec();
                System.out.println( "Result: " + results3 );
                System.out.println( "Expected: [a.e]" );

                if( results3.size() == 1 && results3.contains( "a.e" ) ) {
                        System.out.println( "✓ Test 3 PASSED" );
                } else {
                        System.out.println( "✗ Test 3 FAILED" );
                        System.exit( 1 );
                }

                System.out.println( "\n✓ ALL TESTS PASSED" );
        }
}
