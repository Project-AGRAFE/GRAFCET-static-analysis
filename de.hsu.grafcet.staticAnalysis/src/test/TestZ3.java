package test;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Log;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

public class TestZ3 {

		private String outcome;

		public static void main(String[] args) {
			// Z3 Test
			TestZ3 t = new TestZ3();
			t.simpleExample();

		}


		public void simpleExample()
	    {
	        System.out.println("Z3 Simple Example");
	        
	        {
	            Context ctx = new Context();
	            /* do something with the context */
	            
//	            try {
					doZ3(ctx);
//				} catch (SatisfiabilityCheckFailedException e) {
//					e.printStackTrace();
//				}

	            /* be kind to dispose manually and not wait for the GC. */
	            ctx.close();
	        }
	        Log.close();
	    }
		public void doZ3(Context ctx) {
			BoolExpr x = ctx.mkBoolConst("x");
			BoolExpr y = ctx.mkBoolConst("y");

			Model model = check(ctx, ctx.mkAnd(x, ctx.mkNot(y)), Status.SATISFIABLE);
			
			/* Check if the string name of variable matters
			x = ctx.mkBoolConst("x");
			y = ctx.mkBoolConst("x");

			model = check(ctx, ctx.mkAnd(x, ctx.mkNot(y)), Status.SATISFIABLE);
			System.out.println("x = " + model.evaluate(x, false) + ", y ="
	                + model.evaluate(y, false));
			*/
			
		}

	    private Model check(Context ctx, BoolExpr f, Status sat) {
	        Solver s = ctx.mkSolver();
	        s.add(f);
	        if (s.check() != sat)
	            throw new IllegalArgumentException();
	        if (sat == Status.SATISFIABLE) {
	        	System.out.println("SATISFIABLE");
	            return s.getModel();
	        }
	        else
	            return null;
	    }
	
	
}
