import java.util.Iterator;

class Echo {
	public static void main(String[] args) {
	    StringBuilder b = new StringBuilder();
	    
	    for (int i = 0; i < args.length; i++) {
	      b.append(args[i]);
	      if (i+1 < args.length)
	        b.append("\n");
	    }
	  
		System.out.println(b.toString());
	}
}