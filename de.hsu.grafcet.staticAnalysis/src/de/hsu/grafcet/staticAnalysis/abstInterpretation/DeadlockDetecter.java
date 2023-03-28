package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;

import apron.Abstract1;
import apron.ApronException;
import de.hsu.grafcet.Transition;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import terms.*;

public class DeadlockDetecter {
	
	
	//TODO Grafcet übergeben statt subgraf
	
	//FIXME Kozept so wie implementiert nicht sound. (siehe confluence)
	//TODO SMT wird benötigt
	
	public static String checkDeadlocks (HierarchyOrder hierarchyOrder) throws ApronException{
		String out = "Abstract interpretation based deadlock detection:";
		boolean deadlockGlobal = false;
		for (Subgraf subgraf : ai.getHypergraf().getSubgrafs()) {
			boolean deadlockSubgraf = false;
			String temp = "Partial Grafcet: " + subgraf.getPartialGrafcet().getName();
			for (Vertex vertex : subgraf.getVertices()) {
				List<Edge> downstreamEdges = new ArrayList<Edge>(subgraf.getDownstreamEdges(vertex));
				//all abstract values of downstreamEdges are the same:
				Abstract1 absValueN = ai.getAbstractEnvMap().get(downstreamEdges.get(0));
				System.out.println(Arrays.toString(absValueN.toBox(ai.getMan())));
				Abstract1 absValueConditions = new Abstract1(ai.getMan(), ai.getEnv(), true);
				System.out.println(Arrays.toString(absValueConditions.toBox(ai.getMan())));
				for(Edge edge : downstreamEdges) {
					//using transfer function with top abstract values
					Abstract1 absValueCondition = TransferFunction.transfer(
							edge, new Abstract1(ai.getMan(),  ai.getEnv()), ai.getMan(), ai.getEnv());
					absValueConditions.join(ai.getMan(), absValueCondition);
				}
				System.out.println(Arrays.toString(absValueConditions.toBox(ai.getMan())));
				if(absValueN.isIncluded(ai.getMan(), absValueConditions)){
					//no deadlock detected
				} else {
					//deadlock detected
					deadlockSubgraf = true;
					deadlockGlobal = true;
					temp += "\n\nDeadlock detected regarding transition(s): " + downstreamEdges;
					temp += "\nAbstract values of variables: " + Arrays.toString(absValueN.toBox(ai.getMan()));
					temp += "\nAbstract values of associated transition conditions: " 
							+ Arrays.toString(absValueConditions.toBox(ai.getMan()));
				}
			}
			if (deadlockSubgraf) out += temp;
		}
		if(deadlockGlobal)
			return out;
		else
			return "No Deadlocks detected based onbstract interpretation";
	}
	
	
	private void checkSatisfiabilityTransistion(Term term, List<Tuple<String, Object, Object, Object>> variableBounds){

		Context ctx = new Context();

		BoolExpr boolExpr = (BoolExpr) getTransitionTerm(term, ctx, 0);

		for (Tuple<String, Object, Object, Object> entry : variableBounds) {

			if (entry.getDatatypeVar().equals("bool")) {

				if (!entry.getLowerbound().equals(null)) {

					BoolExpr lbBool = ctx.mkEq(ctx.mkBoolConst(entry.getNameVar()),
							ctx.mkBool((Boolean) entry.getLowerbound()));
					boolExpr = ctx.mkAnd(boolExpr, lbBool);

				}

				if (!entry.getUpperbound().equals(null)) {

					BoolExpr ubBool = ctx.mkEq(ctx.mkBoolConst(entry.getNameVar()),
							ctx.mkBool((Boolean) entry.getUpperbound()));
					boolExpr = ctx.mkAnd(boolExpr, ubBool);

				}

			} else if (entry.getDatatypeVar().equals("int")) {

				if (!entry.getLowerbound().equals(null)) {

					// variable >= lowerbound
					BoolExpr lbInt = ctx.mkGe(ctx.mkIntConst(entry.getNameVar()),
							ctx.mkInt((java.lang.Integer) entry.getLowerbound()));
					boolExpr = ctx.mkAnd(boolExpr, lbInt);

				}

				if (!entry.getUpperbound().equals(null)) {

					// variable <= upperbound
					BoolExpr ubInt = ctx.mkLe(ctx.mkIntConst(entry.getNameVar()),
							ctx.mkInt((java.lang.Integer) entry.getUpperbound()));
					boolExpr = ctx.mkAnd(boolExpr, ubInt);

				}
				
			}

		}

		String model = check(ctx, boolExpr, Status.SATISFIABLE);

//		out.append(lineSeparator);
//		out.append("TransitionId: ");
//		out.append(t.getId());
//		out.append(lineSeparator);
//		out.append("TransitionCondition: ");
//		out.append(boolExpr);
//		out.append(lineSeparator);
//		out.append("Result: ");
//		out.append(lineSeparator);
//		out.append(model);
//		out.append(lineSeparator);

		ctx.close();

	}
	
	private boolean check(Context ctx, BoolExpr f, Status sat) {

		Solver s = ctx.mkSolver();
		s.add(f);

		if (s.check() != sat) {

			return false;
//			return "WARNING: Transition with ID: " + t.getId() + " is not satisfiable";

		}

		if (sat == Status.SATISFIABLE) {

			return true;
			// System.out.println("SATISFIABLE");
//			return s.getModel().toString();

		} else {

			throw new IllegalArgumentException();

		}
	}
	
	public Object getTransitionTerm(Term term, Context ctx, java.lang.Integer sgnlEdge) {

		terms.Sort sort = checkSort(term);

		if (sort instanceof Bool) {

			if (term instanceof Variable) {

				StringBuilder varName = new StringBuilder();
				varName.append(((Variable) term).getVariableDeclaration().getName());

				if (sgnlEdge > 0) {

					// Add marker to negation variable of signalEdge
					varName.append("_");
					varName.append(sgnlEdge.toString());

				}
				return ctx.mkBoolConst(varName.toString());

			} else if (term instanceof Operator) {

				// System.out.println(((Operator) term));

				if (term instanceof And) {

					return ctx.mkAnd((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(BoolExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof Or) {

					return ctx.mkOr((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(BoolExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof Not) {

					return ctx.mkNot((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0));

				} else if (term instanceof Equality) {

					// Check which type of sort the input of the equality operator has
					if (checkSort(getSubterm(term, 0)) instanceof Bool) {

						return ctx.mkEq((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
								(BoolExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

					} else {

						return ctx.mkEq((ArithExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
								(ArithExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

					}

				} else if (term instanceof GreaterThan) {

					return ctx.mkGt((ArithExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(ArithExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof LessThan) {

					return ctx.mkLt((ArithExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(ArithExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof RisingEdge) {

					// The (t-1) Variable is marked with an 1, the regular (t) variable has the 0
					return ctx.mkAnd(ctx.mkNot((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 1)),
							(BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0));

				} else if (term instanceof FallingEdge) {

					// The (t-1) Variable is marked with an 1, the regular (t) variable has the 0
					return ctx.mkAnd((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 1),
							ctx.mkNot((BoolExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0)));

				} else if (term instanceof BooleanConstant) {

					return ctx.mkBool(((BooleanConstant) term).isValue());

				} else {
					throw new IllegalArgumentException();
//					throw new TermIdentificationFailedException(term);

				}

			} else {
				throw new IllegalArgumentException();
//				throw new TermIdentificationFailedException(term);

			}
		} else {

			if (term instanceof Variable) {

				// System.out.println(((Variable) term));
				StringBuilder varName = new StringBuilder();
				varName.append(((Variable) term).getVariableDeclaration().getName());

				if (sgnlEdge > 0) {

					// Add marker to negation variable of signalEdge
					varName.append("_");
					varName.append(sgnlEdge.toString());

				}

				return ctx.mkIntConst(varName.toString());

			} else if (term instanceof Operator) {

				// System.out.println(((Operator) term));

				if (term instanceof Addition) {

					return ctx.mkAdd((ArithExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(ArithExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof Substraction) {

					return ctx.mkSub((ArithExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(ArithExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof Equality) {

					// TODO: Not reachable, equality operator has sort boolean
					return ctx.mkEq((ArithExpr) getTransitionTerm(getSubterm(term, 0), ctx, 0),
							(ArithExpr) getTransitionTerm(getSubterm(term, 1), ctx, 0));

				} else if (term instanceof IntegerConstant) {

					return ctx.mkInt(((IntegerConstant) term).getValue());

				} else {
					throw new IllegalArgumentException();
//					throw new TermIdentificationFailedException(term);
				}

			} else {
				throw new IllegalArgumentException();
//				throw new TermIdentificationFailedException(term);

			}
		}

	}
	public terms.Sort checkSort(Term term) {
		if (term.getSort() instanceof Bool) {
			return term.getSort();
		}
		// It has to be an Integer
		else {
			return term.getSort();
		}
	}
	public Term getSubterm(Term term, int index) {

		return ((Operator) term).getSubterm().get(index);

	}
	
}
