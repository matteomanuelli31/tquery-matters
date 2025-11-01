class TestVivification {
    public static void main(String[] args) {
        Value root = Value.create();
        
        System.out.println("Before getChildren():");
        System.out.println("  root.children().size() = " + root.children().size());
        
        // This will auto-create the field!
        ValueVector vec = root.getChildren("nonexistent");
        
        System.out.println("\nAfter getChildren('nonexistent'):");
        System.out.println("  root.children().size() = " + root.children().size());
        System.out.println("  vec.size() = " + vec.size());
        System.out.println("  root.hasChildren('nonexistent') = " + root.hasChildren("nonexistent"));
        
        // Iterating through empty vec
        System.out.println("\nIterating through empty vec:");
        for(int i = 0; i < vec.size(); i++) {
            System.out.println("  [" + i + "]");  // Won't print anything
        }
        System.out.println("  (no iterations, as expected)");
    }
}
