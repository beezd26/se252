package jsint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/** This class allows you to call any Java method, just by naming it,
 * and doing the dispatch at runtime.
 * @author Peter Norvig, Copyright 1998, peter@norvig.com, <a href="license.txt">license</a>
 * subsequently modified by Jscheme project members
 * licensed under zlib licence (see license.txt)
**/

public class JavaMethod extends Reflector {
	
	private class WhiteList
	{
		public String clazz;
		public String method;	
		
		WhiteList(String c, String m){
			clazz = c;
			method = m;
		}
		
		public String getClazz() {return clazz;}
		public String getMethod() {return method;}
	}

  public static final Object[] ZERO_ARGS = new Object[0];

  private String methodClass;
  /** Parameter/method table for a specific method. **/
  private transient Object[] methodTable;
  private boolean isStatic;
  /** Do we know the Class that this method applies to? **/
  private boolean isSpecific;
  /** Class -> methodTable map. **/
  private transient Hashtable classMethodTable;
  
  public boolean isStatic() { return this.isStatic;}
  
 
  
  /**

     If the method is static then Class c is not null.  For instance
     methods, if Class c is not null, then it is used at construction
     time to create a method table.  Otherwise, the class of the
     method is determined at call time from the target, and the method
     table is constructed then and cached. Examples (see DynamicVariable.java):

      <pre>
      new JavaMethod("getProperties", System.class, true) - static method
      new JavaMethod("put", Hashtable.class,false)        - specific instance method.
      new JavaMethod("put", null, false)                  - unspecified instance method
      </pre>
   **/

 
  private static String WHITE_LIST_FILE_NAME = null;
  
  private static boolean initializedWhiteList = false;
  private static List<WhiteList> whiteList = new LinkedList<WhiteList>();
  
  private static boolean CompileMode = true;
  
   public JavaMethod(String name, Class c, boolean isStatic, boolean isPrivileged) { 
	if(!initializedWhiteList){
		initializeWhiteList();
		initializedWhiteList = true;
	}
	
    this.name = name;
    if (c != null) this.methodClass = c.getName();
    this.isStatic = isStatic;
    this.isSpecific = (c!=null);
    this.minArgs = isStatic ? 0 : 1;
    this.isPrivileged=isPrivileged;
    reset();
   } 
    
    public JavaMethod(String name, Class c, boolean isStatic) { 
      this(name,c,isStatic,false);
    }

    public JavaMethod(String name, Class c) {
      this(name,c,(c!=null));
    }     
    
  public static void setPermissionsFile(String fileName){
	  WHITE_LIST_FILE_NAME = fileName;
  }
  
  public static void turnOffCompileMode(){
	  CompileMode = false;
  }
    
  private void initializeWhiteList(){ 
	    BufferedReader br = null;
	    try {
	    	br = new BufferedReader(new FileReader(WHITE_LIST_FILE_NAME));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            String[] split = line.split(" ");
	            try{
	            	whiteList.add(new WhiteList(split[0], split[1]));
	            } catch (ArrayIndexOutOfBoundsException e){
	            	whiteList.add(new WhiteList(split[0], null));
	            }
	            line = br.readLine();
	        }
	        br.close();
	    } catch (Exception e){
	    	System.out.println(e.toString());
	    }
  }
    
  protected synchronized void reset() {
    if (isSpecific) {
      methodTable = Invoke.methodTable0(Import.classNamed(methodClass),
					name,
					isStatic,
                                        isPrivileged);
      if (methodTable.length == 0) {
          methodTable = null;
          E.warn(  "No such "+ (isStatic?" static ":" instance ") + 
                    " method \"" + name + (isSpecific?("\" in class "+methodClass):""));
      }
    } else classMethodTable = new Hashtable(5);
  }

  public Object[] instanceMethodTable(Class c) {
    Object[] ms = ((Object[]) classMethodTable.get(c));
    if (ms != null) return ms;
    ms = Invoke.methodTable0(c, name, isStatic, isPrivileged);
    if (ms != null && ms.length > 0) {
      classMethodTable.put(c, ms);
      return ms;
    } else return (Object[]) E.error(c + " has no methods for " + this.name);
  }

  /**
     For a static method, args is an Object[] of arguments.
     For an instance method, args is (vector target (vector arguments));
   **/
	public Object apply(Object[] args) {
		try {
			if (!(isSpecific)) {
				Object[] methodTable = instanceMethodTable(args[0].getClass());
				Object[] as = (Object[]) args[1];
				Method m = (Method) Invoke.findMethod(methodTable, as);
				checkMethod(m);
				return Invoke.invokeRawMethod(m, args[0], as);
			} else {
				if (methodTable == null)
					return E.error(this + " has no methods");
				if (isStatic) {
					Method m = (Method) Invoke.findMethod(methodTable, args);
					checkMethod(m);
					return Invoke.invokeRawMethod(m, null, args);
				} else {
					Object[] as = (Object[]) args[1];
					Method m = (Method) Invoke.findMethod(methodTable, as);
					checkMethod(m);
					return Invoke.invokeRawMethod(m, args[0], as);
				}
			}
		} catch (Exception e) {
			return E.error(e.toString());
		}
	}
  
	public void checkMethod(Method m) throws Exception {
		if (CompileMode == false) {
			boolean methodOkToRun = false;

			for (WhiteList wl : whiteList) {
				if ((m.getDeclaringClass().getName().equals(wl.getClazz()) && m
						.getName().equals(wl.getMethod()))
						|| (m.getDeclaringClass().getName()
								.equals(wl.getClazz()) && wl.getMethod() == null)) {
					methodOkToRun = true;
					break;
				}
			}
			if (!methodOkToRun)
				throw new Exception(
						"***** YOU DO NOT HAVE PROPER PRIVILEGES TO RUN THIS JAVA METHOD! *****");
		}
	}

  public Object[] makeArgArray(Object[] code,
                               Evaluator eval,
                               LexicalEnvironment lexenv) {
    if (isStatic) {
      int L = code.length - 1;
      if (L == 0) return ZERO_ARGS;
    
      Object[] args = new Object[L];
      for (int i = 0; i < L; i++)
	args[i] = eval.execute(code[i+1], lexenv);
      return args;
    } else {
      int L = code.length - 2;
      if (L < 0)
	return ((Object[])
		E.error("Wrong number of arguments in application: "
			+ U.stringify(code)));
      Object target = eval.execute(code[1], lexenv);
      if (L == 0) return new Object[] { target, ZERO_ARGS };
    
      Object[] args = new Object[L];
      for (int i = 0; i < L; i++)
	args[i] = eval.execute(code[i+2], lexenv);
      return new Object[] { target, args };
    }
  }
      
  public Object[] makeArgArray (Pair args) {
    if (isStatic) return U.listToVector(args);
    else return new Object[] { args.first, U.listToVector(args.rest)} ;
  }
}



