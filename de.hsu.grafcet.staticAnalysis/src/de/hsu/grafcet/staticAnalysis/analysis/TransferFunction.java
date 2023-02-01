package de.hsu.grafcet.staticAnalysis.analysis;

import java.util.Arrays;
import apron.*;
import de.hsu.grafcet.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import terms.*;

public abstract class TransferFunction {
	
	
	public static Abstract1 transfer(Statement n, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		Abstract1 copyabstractValueN = new Abstract1(man, abstractValueN);
		if (n instanceof Edge) {
			return transferEdge((Edge) n, copyabstractValueN, man, env);
		}else if (n instanceof Vertex) {
			return transferVertex((Vertex) n, copyabstractValueN, man, env);
		}else {
			throw new IllegalArgumentException("case not covered: " + n.toString());
		}
	}
	
	
	private static Abstract1 transferEdge(Edge n, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		Term term = n.getTransition().getTerm();
		return  recusiveAbstrsactFromTermBuilder(term, abstractValueN, man, env);
	}
	
	private static  Abstract1 recusiveAbstrsactFromTermBuilder(Term term, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
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
			Abstract1 abstractA = recusiveAbstrsactFromTermBuilder(((And) term).getSubterm().get(0), abstractValueN, man, env);
			Abstract1 abstractB = recusiveAbstrsactFromTermBuilder(((And) term).getSubterm().get(1), abstractValueN, man, env);
			abstractA.join(man, abstractB);
			out = abstractValueN.meetCopy(man, abstractA);
			
		}else if (term instanceof Not) {
			Not notTerm = (Not) term;
			if (notTerm.getSubterm().get(0) instanceof Variable) {
				Variable var = (Variable) notTerm.getSubterm().get(0);
				out = abstractValueN.meetCopy(man, abstractEnvFromVariableExpr(var, abstractValueN, true, man, env));
			} else {
				throw new IllegalArgumentException("NOT-term is only allowed on Boolean variables");
			}
		}else if (term instanceof RisingEdge) {
			//TODO
			throw new NotImplementedException();
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
			throw new IllegalArgumentException("Case not covered");
		}
		return out;
	}
	
	private static Abstract1 abstractEnvFromVariableExpr(Variable var, Abstract1 abstractEnv, boolean isNegated, Manager man, Environment env) throws ApronException {
		Abstract1 out;
		int val = 1;
		if (isNegated) val = 0;
		if ((var).getVariableDeclaration().getSort() instanceof Bool){
			if ((var).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.INPUT)) {
				out = new Abstract1(man, env);
			} else {
				Texpr1Node subTxprNode = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
						new Texpr1VarNode((var).getVariableDeclaration().getName()),
						new Texpr1CstNode(new MpqScalar(val)));
				Texpr1Intern subTxprInt = new Texpr1Intern(env, subTxprNode);
				Tcons1 expr = new Tcons1(Tcons1.EQ, subTxprInt);
				out = abstractEnv.meetCopy(man, expr);
			}
		}else {
			throw new IllegalArgumentException("incorrect syntax: " + var);
		}
		return out;
	}
	
	private static Abstract1 transferVertex (Vertex n, Abstract1 abstractValueN, Manager man, Environment env) throws ApronException {
		for (ActionType actionType : n.getActionTypes()) {
			if (actionType instanceof StoredAction) {
				StoredAction storedAction = (StoredAction) actionType;
				switch (storedAction.getStoredActionType().getValue()) {
				case StoredActionType.ACTIVATION_VALUE: {
					Term value = storedAction.getValue();
					if (storedAction.getVariable().getVariableDeclaration().getSort() instanceof Bool) {
						Texpr1Node texprNode;
						if (((BooleanConstant)value).isValue()) {
							texprNode = new Texpr1CstNode(new Interval(1,1));
						} else {
							texprNode = new Texpr1CstNode(new Interval(0,0));
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
					break;
				} case StoredActionType.DEACTIVATION_VALUE: {
					if (storedAction.getVariable().getVariableDeclaration().getSort() instanceof Bool) {
						
					}else {
						
					}
					//covered with hypergraf transformation
					throw new IllegalArgumentException("normalization of actions failed");
				} case StoredActionType.EVENT_VALUE: {
					if (storedAction.getVariable().getVariableDeclaration().getSort() instanceof Bool) {
						
					}else {
						
					}
					
					//abstrakten Wert für Bedingung berechnen
					//meet mit Wert für 1 Hilfsschritt s1'
					//abstrakten Wert für s1' berechnen
					
					//abstrakten Wert für S1 berechnen (join mit t1)
					//join abs s1' und abs s1
					//return
					
					//Es ist zu beachten, dass Transition ta keinen Einfluss hat, da danach join ausgeführt wird
					
					//Also:
					//"aktueller Wert join anwendung der Aktion"
					
					throw new NotImplementedException(); //Es ist wohl unwarscheinlich, dass in der Bedingung interne Variablen auftauchen
					
					
				}
				default:
					throw new IllegalArgumentException("Unexpected value: " + storedAction.getStoredActionType());
				}
				
			}else {
				//throw new NotImplementedException();
			}
		}
		return abstractValueN;
	}
	
	
	private static Texpr1Node recursiveLinearExprBuilder(Term term) {
		if (term instanceof Variable) {
			if (((Variable)term).getVariableDeclaration().getVariableDeclarationType().equals(VariableDeclarationType.INPUT)) {
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
					recursiveLinearExprBuilder(((Addition)term).getSubterm().get(0)), 
					recursiveLinearExprBuilder(((Addition)term).getSubterm().get(1)));
		}
		throw new IllegalArgumentException("incorrect syntax or error in algorithm");
	}
	
	private static Tcons1 predicateConsBuilder(Operator pedicateOperator, Environment env) {
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

}
