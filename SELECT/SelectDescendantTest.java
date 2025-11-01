import java.util.List;

public class SelectDescendantTest {
        public static void main( String[] args ) {
                // Create structure:
                // a.b.c.d = 5
                // a.d = 5
                // a.c = 5

                Value root = Value.create();
                Value a = root.getChildren( "a" ).first();

                // a.b.c.d = 5
                Value b = a.getChildren( "b" ).first();
                Value c = b.getChildren( "c" ).first();
                c.getChildren( "d" ).first().setValue( 5 );

                // a.d = 5
                a.getChildren( "d" ).first().setValue( 5 );

                // a.c = 5
                a.getChildren( "c" ).first().setValue( 5 );

                // Verify structure
                System.out.println( "Structure created:" );
                System.out.println( "  a.b.c.d = " + root.getChildren( "a" ).first()
                        .getChildren( "b" ).first()
                        .getChildren( "c" ).first()
                        .getChildren( "d" ).first().intValue() );
                System.out.println( "  a.d = " + root.getChildren( "a" ).first()
                        .getChildren( "d" ).first().intValue() );
                System.out.println( "  a.c = " + root.getChildren( "a" ).first()
                        .getChildren( "c" ).first().intValue() );

                // Execute: SELECT ..d FROM root WHERE . = 5
                // Should find all fields named "d" with value 5 at any depth
                System.out.println( "\nQuery: SELECT ..d FROM root WHERE . = 5" );
                List<String> results = new SelectBuilder()
                        .select( "..d" )
                        .from( root )
                        .where( ". = 5" )
                        .exec();

                System.out.println( "\nResults:" );
                for( String path : results ) {
                        System.out.println( "  " + path );
                }

                // Expected: ["a.b.c.d", "a.d"]
                System.out.println( "\nExpected: [a.b.c.d, a.d]" );

                if( results.size() == 2 &&
                    results.contains( "a.b.c.d" ) &&
                    results.contains( "a.d" ) ) {
                        System.out.println( "✓ TEST PASSED" );
                        System.exit( 0 );
                } else {
                        System.out.println( "✗ TEST FAILED" );
                        System.out.println( "Got: " + results );
                        System.exit( 1 );
                }
        }
}
