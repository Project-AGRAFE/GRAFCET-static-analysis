package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import apron.Environment;
import apron.Interval;
import apron.Manager;
import apron.MpqScalar;
import apron.NotImplementedException;
import apron.Tcons1;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import apron.Var;
import de.hsu.grafcet.GrafcetFactory;
import de.hsu.grafcet.Transition;
import de.hsu.grafcet.VariableDeclarationContainer;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import terms.*;
import terms.Integer;

public class FlawedTransitionDetecter extends Detecter{

	private String result = "\n\n== Results FlawedTransitionDetecter: ==";
	
	public FlawedTransitionDetecter(HierarchyOrder hierarchyOrder) {
		super(hierarchyOrder);
		env = Util.generateEnvironment(this.hierarchyOrder.getHypergraf(), true); 
		
		
	}
	
	@Override
	public void runAnalysis() throws ApronException {
		for (HierarchyDependency dependency: hierarchyOrder.getDependencies()) {
			for (Edge edge : dependency.getInferior().getEdges()) {
				
				Abstract1 absValueNInputs = Util.addInputsToEnvAndSubstitute(hierarchyOrder.getAbstract1FromStatementAndDependency(dependency, edge), 
						env, hierarchyOrder.getHypergraf().getGlobalGrafcet().getVariableDeclarationContainer());
				
				if (detectAlwaysFiring(edge.getTransition().getTerm(), absValueNInputs)) {
					result += "\nERROR: Transition " + edge.getId() + " always fires."; 
				}
				if (detectNonFiring(edge.getTransition().getTerm(), absValueNInputs)) {
					result += "\nERROR: Transition " + edge.getId() + " never fires."; 
				}
			}
			//detect possible deadlock or livelocks due to internal variables
			for (Vertex vertex : dependency.getInferior().getVertices()) {
				List<Edge> downstreamEdges = new ArrayList<Edge>(dependency.getInferior().getDownstreamEdges(vertex));
				if(downstreamEdges.size() > 0) {
					List<Term> terms = new ArrayList<Term>();
					for(Edge edge : downstreamEdges) {
						terms.add(edge.getTransition().getTerm());
					}
					Model model = null;
					Abstract1 absValueNInputs = Util.addInputsToEnvAndSubstitute(hierarchyOrder.getAbstract1FromStatementAndDependency(dependency, downstreamEdges.get(0)),
							env, hierarchyOrder.getHypergraf().getGlobalGrafcet().getVariableDeclarationContainer());
					checkSatisfiabilityDisjunction(terms, absValueNInputs, model);
					if (model != null) {
						result += "\nWARNING: Downstream transition(s) after step " + vertex.getId() + " do(es) not cover the internal variable values";
					}
				}
			}
		}
	}

	@Override
	public String getResults() {
		return result;
	}
	
	private boolean detectNonFiring(Term term, Abstract1 absValueN) throws ApronException {
		TransferFunction trans = new TransferFunction(null, absValueN, new Box(), env);
		trans.setModelInputVariabels(true);
		Abstract1 result = trans.transferTerm(term);
		if (result.isBottom(man)) {
			return true; //error detected
		}
		return false;
	}
	

	private boolean detectAlwaysFiring(Term term, Abstract1 absValueNInputs) throws ApronException {
		TransferFunction trans = new TransferFunction(null, absValueNInputs, new Box(), env);
		trans.setModelInputVariabels(true);
		Abstract1 result = trans.transferTerm(term);
		if (absValueNInputs.isIncluded(man, result)) { 
			return true; //error detected
		}
		return false;
	}
		
	
	
	
	/*
	public static String checkDeadlocks (HierarchyOrder hierarchyOrder) throws ApronException{
		String out = "Abstract interpretation based deadlock detection:";
//		boolean deadlockGlobal = false;
		TermsFactory fac = TermsFactory.eINSTANCE;
		for (HierarchyDependency dependency: hierarchyOrder.getDependencies()) {
//			boolean deadlockSubgraf = false;
//			String temp = "Partial Grafcet: " + subgraf.getPartialGrafcet().getName();
			for (Vertex vertex : dependency.getInferior().getVertices()) {
				List<Edge> downstreamEdges = new ArrayList<Edge>(dependency.getInferior().getDownstreamEdges(vertex));
				Map<Edge, Abstract1> absEdgeValues = new HashMap<Edge, Abstract1>();
				Abstract1 absValueN = hierarchyOrder.getAbstract1FromStatement(dependency, downstreamEdges.get(0));
				
				for(Edge edge : downstreamEdges) {
					Not negatedTerm = fac.createNot();
//					negatedTerm.getSubterm().add(edge.getTransition().getTerm()); 
					TransferFunction trans = new TransferFunction(null, absValueN, new Box(), hierarchyOrder.getEnv());
					absEdgeValues.put(edge, trans.transferTerm(edge.getTransition().getTerm()));
					
					//negation:
					
					Texpr1Node subTxprNode = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
							recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(0)),
							recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(1)));
					Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
					return new Tcons1(Tcons1.EQ, subTxprInt);
					
					Texpr1Node subTxprNode = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
							new Texpr1VarNode((var).getVariableDeclaration().getName()),
							new Texpr1CstNode(new MpqScalar(0)));
					Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
					Tcons1 expr = new Tcons1(Tcons1.EQ, subTxprInt);
					out = abstractEnv.meetCopy(man, expr);
				}
				}
				
				
				
				
				
				
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
	
	*/
	
	/**
	 * compares abstract values of internal variables to disjunctions (might have only one transitions). 
	 * If values form the intervals of internal varibales are satisfy the negated transition true is returned. 
	 * This might lead to a deadlock when it is not intended that the variable values are not in the bounds of the transition condition(s)
	 * 
	 * 
	 * @param terms List of terms from the dijunction
	 * @param absValueN abstract variable values before a transition form the disjunction fires
	 * @param model	If the expression is SAT the solution will be assigned to @param model
	 * @return true if expression is SAT
	 * @throws ApronException
	 */
	
	private boolean checkSatisfiabilityDisjunction(List<Term> terms, Abstract1 absValueN, Model model) throws ApronException {
		List<BoolExpr> negatedTerms = new ArrayList<BoolExpr>();
		Context ctx = new Context();
		for (Term term : terms) {
			BoolExpr z3term = ctx.mkNot((BoolExpr) getTransitionTerm(term, ctx, 0));
			negatedTerms.add(z3term);
		}
		List<BoolExpr> boundsInternalVariables = getZ3BoundsFromAbstrValues(collectInternalVariables(), absValueN, ctx);
//		Set<VariableDeclaration> varDecls = collectInternalVariables();
				
		BoolExpr disjunctionExpr = ctx.mkBool(true);
		for (BoolExpr e : negatedTerms) {
			disjunctionExpr = ctx.mkAnd(disjunctionExpr, e);
		}
		for (BoolExpr e : boundsInternalVariables) {
			disjunctionExpr = ctx.mkAnd(disjunctionExpr, e);
		}
		return checkForSat(ctx, disjunctionExpr, model);
	}
	
	private Set<VariableDeclaration> collectInternalVariables() {
		Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
		for (VariableDeclaration var : hierarchyOrder.getHypergraf().getGlobalGrafcet().getVariableDeclarationContainer().getVariableDeclarations()) {
			//collect internal variables
			if (var.getVariableDeclarationType().equals(VariableDeclarationType.INTERNAL)) {
				variableDeclarationSet.add(var);
			}
		}
		return variableDeclarationSet;
	}
	private List<BoolExpr> getZ3BoundsFromAbstrValues(Set<VariableDeclaration> varDecls, Abstract1 absValues, Context ctx) throws ApronException{
		List<BoolExpr> boundsInternalVariables = new ArrayList<BoolExpr>(); 
		
		for (VariableDeclaration varDecl : varDecls) {
			Interval interval = absValues.getBound(man, varDecl.getName());
			double lowerBound[] = {0};
			double upperBound[] = {0};
			interval.inf.toDouble(lowerBound, 0);
			interval.sup.toDouble(upperBound, 0);
			int lowerBoundInt = (int) lowerBound[0];
			int upperBoundInt = (int) upperBound[0];

			// variable >= lowerbound
			BoolExpr boundExpr = ctx.mkGe(ctx.mkIntConst(varDecl.getName()),
					ctx.mkInt(lowerBoundInt));
			// variable <= upperbound
			BoolExpr upperBoundExpr = ctx.mkLe(ctx.mkIntConst(varDecl.getName()),
					ctx.mkInt(upperBoundInt));
			boundExpr = ctx.mkAnd(boundExpr, upperBoundExpr);
			boundsInternalVariables.add(boundExpr);
		}
		return boundsInternalVariables;
	}
	/**
	 * checks if @param expr is SAT. If so the result is assigned to @param model 
	 * @param ctx
	 * @param expr
	 * @param model
	 * @return
	 */
	private boolean checkForSat(Context ctx, BoolExpr expr, Model model) {
		Solver solver = ctx.mkSolver();
		solver.add(expr);
		if (solver.check() != Status.SATISFIABLE) {
			return false;
		}else if (solver.check() == Status.SATISFIABLE) {
			model = solver.getModel();
			return true;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public Expr getTransitionTerm(Term term, Context ctx, java.lang.Integer sgnlEdge) {

//		terms.Sort sort = checkSort(term);

//		if (sort instanceof Bool) {

			if (term instanceof Variable) {
				if(((Variable)term).getVariableDeclaration().getSort() instanceof Bool) {
					StringBuilder varName = new StringBuilder();
					varName.append(((Variable) term).getVariableDeclaration().getName());
	
					if (sgnlEdge > 0) {
	
						// Add marker to negation variable of signalEdge
						varName.append("_");
						varName.append(sgnlEdge.toString());
	
					}
					return ctx.mkBoolConst(varName.toString());
				}else if(((Variable)term).getVariableDeclaration().getSort() instanceof Integer) {
						// System.out.println(((Variable) term));
						StringBuilder varName = new StringBuilder();
						varName.append(((Variable) term).getVariableDeclaration().getName());

						if (sgnlEdge > 0) {

							// Add marker to negation variable of signalEdge
							varName.append("_");
							varName.append(sgnlEdge.toString());

						}

						return ctx.mkIntConst(varName.toString());
				} else {
					throw new IllegalArgumentException();
				}

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

				} 
//				else {
//					throw new IllegalArgumentException();
////					throw new TermIdentificationFailedException(term);
//
//				}

//			} else 
//		} else {

//			if (term instanceof Variable) {
//
//				// System.out.println(((Variable) term));
//				StringBuilder varName = new StringBuilder();
//				varName.append(((Variable) term).getVariableDeclaration().getName());
//
//				if (sgnlEdge > 0) {
//
//					// Add marker to negation variable of signalEdge
//					varName.append("_");
//					varName.append(sgnlEdge.toString());
//
//				}
//
//				return ctx.mkIntConst(varName.toString());

//			} else if (term instanceof Operator) {

				// System.out.println(((Operator) term));

				else if (term instanceof Addition) {

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
//		}

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
