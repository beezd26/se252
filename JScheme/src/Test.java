import jscheme.JScheme;



public class Test {
	public static void main(String[] args) {
		final JScheme js = new JScheme ("whitelist.txt");
		js.load("(define (main PrintStream) (.println PrintStream \"Hello world!\"))");
    	js.call("main", System.out);
	}
}