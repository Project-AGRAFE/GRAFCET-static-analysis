package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import apron.*;
import de.hsu.grafcet.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import terms.*;

public class TransferFunction {
	
	private Statement n;
	private Abstract1 abstractValueN;
	private Manager man;
	private Environment env;
	//Abstract1 interfaceIn; 	//joined interface for multi-threading approach
	//Map<Statement, Abstract1> interfaceOut = new HashMap<Statement, Abstract1>(); //abstractEnv for every statement
	private Abstract1 interfaceEntry;
	private boolean isInterface = false;
	/**
	 * normal impelementation returns top element if an input variable occurs. The flag returns the actual value. 
	 */
	private boolean modelInputVariabels = false;  
	
	public TransferFunction(Statement n, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		super();
		this.n = n;
		this.abstractValueN = new Abstract1(man, abstractValueN);
		this.man = man;
		this.env = env;
	}

	public TransferFunction(Statement n, Abstract1 abstractValueN, Manager man, Environment env, Abstract1 interfaceIn) throws ApronException {
		this(n, abstractValueN, man, env);
		//this.interfaceIn = interfaceIn;
		isInterface = true;
		//apply interface  iff n is reachable (Env(n) != bot)
		if (!abstractValueN.isBottom(man)) {
			this.abstractValueN.join(man, interfaceIn);
		}		
	}
	
	
	public Abstract1 transferInterface() throws ApronException {
		if (isInterface) {
			return transfer();
		} else {
			throw new IllegalArgumentException("Wrong initialization; interface is not set");
		}
	}

	
	/**
	 * returns abstract value after a corresponding vertex with an associated action is executed, null otherwise
	 * @return
	 */
	public Abstract1 getInterferenceOutN() {
		return interfaceEntry;
	}

	/**
	 * Statement n will be ignored. Instead term will be applied on abstractValueN set via constructor
	 * @return
	 * @throws ApronException 
	 */
	public Abstract1 transferTerm(Term term) throws ApronException{
		return recusiveAbstrsactFromTermBuilder(term, abstractValueN, man, env);
	}

	private Abstract1 transfer() throws ApronException {
		Abstract1 copyabstractValueN = new Abstract1(man, abstractValueN);
		if (n instanceof Edge) {
			return transferEdge((Edge) n, copyabstractValueN, man, env);
		}else if (n instanceof Vertex) {
			return transferVertex((Vertex) n, copyabstractValueN, man, env);
		}else {
			throw new IllegalArgumentException("case not covered: " + n.toString());
		}
	}
	
	
	private Abstract1 transferEdge(Edge n, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		Term term = n.getTransition().getTerm();
		return  recusiveAbstrsactFromTermBuilder(term, abstractValueN, man, env);
	}
	
	private Abstract1 recusiveAbstrsactFromTermBuilder(Term term, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		Abstract1 out;
		
		boolean manageableEquality = true;
		if (term instanceof Equality) {
			Equality eq = (Equality) term;
			for (Term st : eq.getSubterm()) {
				if (!(st instanceof Variable ||
						st instanceof Substraction ||
						st instanceof Addition ||
						st instanceof IntegerConstant)) {
					manageableEquality = false;
				}
			}
		}
		
		if (term instanceof GreaterThan ||
				term instanceof LessThan ||
				(term instanceof Equality && manageableEquality)) {
			Tcons1 expr = predicateConsBuilder((Operator)term, env);
			out = abstractValueN.meetCopy(man, expr);
		} else if (term instanceof And) {
			Abstract1 abstractA = recusiveAbstrsactFromTermBuilder(((And) term).getSubterm().get(0), abstractValueN, man, env);
			Abstract1 abstractB = recusiveAbstrsactFromTermBuilder(((And) term).getSubterm().get(1), abstractValueN, man, env);
			abstractA.meet(man, abstractB);
			out = abstractValueN.meetCopy(man, abstractA); //final meet mit Wert muss nicht rekursiv ausgeführt werden.
		}else if (term instanceof Or) {
			Abstract1 abstractA = recusiveAbstrsactFromTermBuilder(((Or) term).getSubterm().get(0), abstractValueN, man, env);
			Abstract1 abstractB = recusiveAbstrsactFromTermBuilder(((Or) term).getSubterm().get(1), abstractValueN, man, env);
			abstractA.join(man, abstractB);
			out = abstractValueN.meetCopy(man, abstractA);
			
		}else if (term instanceof Not) {
			Not notTerm = (Not) term;
			if (notTerm.getSubterm().get(0) instanceof Variable) {
				Variable var = (Variable) notTerm.getSubterm().get(0);
				out = abstractValueN.meetCopy(man, abstractEnvFromVariableExpr(var, abstractValueN, true, man, env));
			} else {
				//TODO unsound implementation, do not use!
				//throw new IllegalArgumentException("NOT-term is only allowed on Boolean variables");
				out = abstractValueN.meetCopy(man, new Abstract1(man, env));
			}
		}else if (term instanceof RisingEdge) {
			//TODO test pls
			if (!(((RisingEdge) term).getSubterm().get(0) instanceof VariableDeclaration)){
				out = abstractValueN.meetCopy(man, new Abstract1(man, env)); 
				//TODO nicht wirklich implementiert
				//throw new IllegalArgumentException("RinsingEdge-term is only allowed on boolean variables");
			} else {
				RisingEdge rETerm = (RisingEdge) term; 
				Variable var = (Variable) rETerm.getSubterm().get(0);
				Abstract1 varPos = abstractValueN.meetCopy(man, abstractEnvFromVariableExpr(var, abstractValueN, false, man, env));
				Abstract1 varNeg = abstractValueN.meetCopy(man, abstractEnvFromVariableExpr(var, abstractValueN, true, man, env));
				
				if (!varPos.isBottom(man) && !varNeg.isBottom(man)) {
					out = varPos;
				} else {
					out = abstractValueN.meetCopy(man, new Abstract1(man, env, true));
				}
			}
		}else if (term instanceof FallingEdge) {
			//TODO
			throw new NotImplementedException();
		}else if (term instanceof BooleanConstant) {
			if (((BooleanConstant)term).isValue()) {
				//top
				out = abstractValueN.meetCopy(man, new Abstract1(man, env));
			} else {
				//bottom
				out = abstractValueN.meetCopy(man, new Abstract1(man, env, true));
			}
		}else if (term instanceof Variable) {
			if (((Variable) term).getVariableDeclaration().getSort() instanceof Bool){
				out = abstractValueN.meetCopy(man, abstractEnvFromVariableExpr((Variable) term, abstractValueN, false, man, env));
			}else {
				throw new IllegalArgumentException("incorrect syntax: " + term);
			}
		} else if (term instanceof Equality && !manageableEquality) {
			throw new NotImplementedException();
		} else {
			//throw new IllegalArgumentException("Case not covered");
			//term might be null if not set
			out = abstractValueN.meetCopy(man, new Abstract1(man, env));
		}
		return out;
	}
	
	private Abstract1 abstractEnvFromVariableExpr(Variable var, Abstract1 abstractValueN, boolean isNegated, Manager man, Environment env) throws ApronException {
		Abstract1 out;
		int val = 1;
		if (isNegated) val = 0;
		if ((var).getVariableDeclaration().getSort() instanceof Bool){
			if ((var).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.INPUT) && !modelInputVariabels) {
				out = new Abstract1(man, env);
			} else if ((var).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.INTERNAL) ||
					((var).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.INPUT) && modelInputVariabels)){
				Texpr1Node subTxprNode = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
						new Texpr1VarNode((var).getVariableDeclaration().getName()),
						new Texpr1CstNode(new MpqScalar(val)));
				Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
				Tcons1 expr = new Tcons1(Tcons1.EQ, subTxprInt);
				out = abstractValueN.meetCopy(man, expr);
			} else if ((var).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.STEP)) {
				//TODO: Überapproximation!
				out = abstractValueN.meetCopy(man, new Abstract1(man, env)); 
				//throw new NotImplementedException();
			} else {
				throw new IllegalArgumentException("Variable of type " + (var).getVariableDeclaration().getVariableDeclarationType() + " not expected.");
			}
			
		}else {
			throw new IllegalArgumentException("incorrect syntax: " + var);
		}
		return out;
	}
	
	private Abstract1 transferVertex (Vertex n, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		for (ActionType actionType : n.getActionTypes()) {
			if (actionType instanceof StoredAction) {
				StoredAction storedAction = (StoredAction) actionType;
				if (storedAction.getStoredActionType().getValue() == StoredActionType.ACTIVATION_VALUE ||
						//stored Action by deactivation handled by transformation
						storedAction.getStoredActionType().getValue() == StoredActionType.DEACTIVATION_VALUE ||
						storedAction.getStoredActionType().getValue() == StoredActionType.EVENT_VALUE) { //TODO: Diese Zeile ist wieder zu entfernen
					Term value = storedAction.getValue();
					if (storedAction.getVariable().getVariableDeclaration().getSort() instanceof Bool) {
						Texpr1Node texprNode;
						if (((BooleanConstant)value).isValue()) {
							texprNode = new Texpr1CstNode(new Interval(1,1));
						} else if (!((BooleanConstant)value).isValue()) {
							texprNode = new Texpr1CstNode(new Interval(0,0)); 
						} else {
							throw new NotImplementedException();
						}
						Texpr1Intern texprIntern = new Texpr1Intern(env, texprNode);
						abstractValueN.assign(man, 
								storedAction.getVariable().getVariableDeclaration().getName(),
								texprIntern, null);
					}else {
						Texpr1Node texprNode = recursiveLinearExprBuilder(value);
						Texpr1Intern texprIntern = new Texpr1Intern(env, texprNode);
						abstractValueN.assign(man, 
								storedAction.getVariable().getVariableDeclaration().getName(),
								texprIntern, null);
					}
					
				//} else if (storedAction.getStoredActionType().getValue() == StoredActionType.EVENT_VALUE) { 
				//	new NotImplementedException(); //Es ist wohl unwarscheinlich, dass in der Bedingung interne Variablen auftauchen
					
				}else {
					throw new IllegalArgumentException("Unexpected value: " + storedAction.getStoredActionType());
				}
				
				setInterfaceEntry(n, storedAction.getVariable().getVariableDeclaration().getName(), abstractValueN);
			}else {
				//throw new NotImplementedException();
			}
			

		}
		return abstractValueN;
	}
	
	private void setInterfaceEntry(Statement n, String keepValueVar, Abstract1 abstractValueN) throws ApronException {
		Abstract1 newInterfaceEntry = new Abstract1(man, abstractValueN);
		for (Var v : env.getVars()) {									//set everything to bot except abstract value of v
			if (v.toString() != keepValueVar) {
				Texpr1Node exprN = new Texpr1CstNode(new Interval(1, -1));
				Texpr1Intern exprI = new Texpr1Intern(env, exprN);
				newInterfaceEntry.assign(man, v, exprI, null);
			}
		}
		if (interfaceEntry != null) {
			interfaceEntry.join(man, newInterfaceEntry);	//vertex could write multiple variables and therefore setInterfaceEntry is called multiple times
		} else {
			interfaceEntry = newInterfaceEntry;
		}
		//System.out.println(Arrays.toString(interfaceEntry.toBox(man)));
	}
	
	private Texpr1Node recursiveLinearExprBuilder(Term term) {
		if (term instanceof Variable) {
			if (((Variable)term).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.INPUT) && !modelInputVariabels) {
				MpfrScalar inf = new MpfrScalar();
				MpfrScalar fin = new MpfrScalar();
				inf.setInfty(1);
				fin.setInfty(-1);		//TODO testen!
				return new Texpr1CstNode(new Interval(fin, inf)); 
			} else {
				return new Texpr1VarNode(((Variable) term).getVariableDeclaration().getName());
			}
		} else if (term instanceof IntegerConstant) {
			return new Texpr1CstNode(new MpqScalar(((IntegerConstant)term).getValue()));
		}else if (term instanceof Addition) {
			return new Texpr1BinNode(Texpr1BinNode.OP_ADD,
					recursiveLinearExprBuilder(((Addition)term).getSubterm().get(0)), 
					recursiveLinearExprBuilder(((Addition)term).getSubterm().get(1)));
		}else if (term instanceof Substraction) {
			return new Texpr1BinNode(Texpr1BinNode.OP_SUB,
					recursiveLinearExprBuilder(((Substraction)term).getSubterm().get(0)), 
					recursiveLinearExprBuilder(((Substraction)term).getSubterm().get(1)));
		}
		throw new IllegalArgumentException("incorrect syntax or error in algorithm");
	}
	
	protected Tcons1 predicateConsBuilder(Operator pedicateOperator, Environment env) {
		if (pedicateOperator instanceof GreaterThan) {
			Texpr1Node subTxprNode = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
					recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(0)),
					recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(1)));
			Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
			return new Tcons1(Tcons1.SUP, subTxprInt);
		} else if (pedicateOperator instanceof LessThan) {
			Texpr1Node subTxprNode = new Texpr1UnNode(Texpr1UnNode.OP_NEG,
					new Texpr1BinNode(Texpr1BinNode.OP_SUB,
							recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(0)),
							recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(1))));
			Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
			return new Tcons1(Tcons1.SUP, subTxprInt);
		} else if (pedicateOperator instanceof Equality) {
			Texpr1Node subTxprNode = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
					recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(0)),
					recursiveLinearExprBuilder(pedicateOperator.getSubterm().get(1)));
			Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
			return new Tcons1(Tcons1.EQ, subTxprInt);
		} else {
			throw new IllegalArgumentException();
		}
	}


	public void setModelInputVariabels(boolean modelInputVariabels) {
		this.modelInputVariabels = modelInputVariabels;
	}
}
