include "console.iol"

main {
    // Test 1: Are a.b and a.b[0] the same?
    a.b = 5;
    println@Console( "a.b = " + a.b )();
    println@Console( "a.b[0] = " + a.b[0] )();

    if ( a.b == a.b[0] ) {
        println@Console( "✓ a.b == a.b[0]" )()
    } else {
        println@Console( "✗ a.b != a.b[0]" )()
    };

    // Test 2: What happens with arrays?
    c.d = 10;
    c.d[1] = 20;
    c.d[2] = 30;

    println@Console( "\nc.d = " + c.d )();
    println@Console( "c.d[0] = " + c.d[0] )();
    println@Console( "c.d[1] = " + c.d[1] )();
    println@Console( "c.d[2] = " + c.d[2] )();
    println@Console( "c.d size: " + #c.d )()
}
