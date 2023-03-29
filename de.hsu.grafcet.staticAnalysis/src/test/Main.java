package test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import apron.*;
import de.hsu.grafcet.Grafcet;
import de.hsu.grafcet.staticAnalysis.abstInterpretation.Util;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;

public class Main {

	public static void main(String[] args) throws ApronException {

		
		//createPartialAbstract1ForInterface();
		
		//For testing APRON:
		//Test.testDomain(new Box());
//		minimalApronExample();
		
		//abstract0Bug();

	}
	

	public static void scalabilityTest() throws ApronException {
		
		System.out.println("variables/actions; loops; length ; time in ms; number of statements");
		
		for (int i = 300; i <= 400; i = i + 50) {
			int loops = 1;
			int length = 25;
			int actions = i;
			
			GrafcetTestInstanceGenerator gtig = new GrafcetTestInstanceGenerator();
			gtig.addPGwLoops(loops, length);
			gtig.addStoredActions(actions);
			Grafcet g = gtig.getGrafcet();
			
			HierarchyOrder ho = new HierarchyOrder(g);
			long start = System.currentTimeMillis();
			ho.runAbstractInterpretation();
			long end = System.currentTimeMillis();
			//System.out.println(ho.getAIFullLog());
			long result = end- start;
			System.out.println((actions + loops) + "; " + loops + "; " + length + "; " + result + "; "+ loops * (2 + length * 4));
		}
	}
	
	
	public static void abstract0Bug() throws ApronException{
		Manager man = new Box();
		
		Interval[] itv = {new Interval(1, 1), new Interval(1, -1)};
		Abstract0 abs = new Abstract0(man, 2, 0, itv);
		
		System.out.println(Arrays.toString(itv));
		System.out.println(Arrays.toString(abs.toBox(man)));
	}
	
	public static void createPartialAbstract1ForInterface() throws ApronException{
		
		
		
		Manager man = new Box();
		String[] varNames = { "k1", "k2" };
		Environment env = new Environment(varNames, new String[] {});
		
		Var[] vars1 = {new StringVar("k1")};
		Interval[] it1 = {new Interval(1, 1)};
	
		Abstract1 abs = new Abstract1(man, env, vars1, it1);
		System.out.println(Arrays.toString(abs.toBox(man)));
		
		
		
		
		//TODO: folgendes als Funktion verpacken und in transfer function einbauen
		Abstract1 abs2 = new Abstract1(man, abs);
		String keepValueVar = "k1";
		for (Var v : env.getVars()) {
			if (v.toString() != keepValueVar) {
				Texpr1Node exprN = new Texpr1CstNode(new Interval(1, -1));
				Texpr1Intern exprI = new Texpr1Intern(env, exprN);
				abs2.assign(man, v, exprI, null);
			}
		}
		
		
		System.out.println(Arrays.toString(abs2.toBox(man)));
		
		
		
//		System.out.println(Arrays.toString(env.getVars()));
//		System.out.println(Arrays.toString(it1));
////		System.out.println(Arrays.toString(interfaceEntry1.toBox(man)));
//		System.out.println(Arrays.toString(it2));
//		System.out.println(Arrays.toString(interfaceEntry2.toBox(man)));
//		
//		
////		Abstract1 abs;
//		String var = "k1";
//		
//		
//		Map<Statement, Abstract1> interfaceOut = new HashMap<Statement, Abstract1>(); 
//		
//		
//		Interval[] initialBox2 = new Interval[env.getVars().length];
//
//		for (int i = 0; i < initialBox2.length; i++) {
//			initialBox2[i] = new Interval(0, 0);
//		}
//		Abstract1 absN = new Abstract1(man, env, env.getVars(), initialBox2);
//
//		
//		System.out.println(Arrays.toString(env.getVars()));
//		System.out.println(Arrays.toString(initialBox2));
//		System.out.println(Arrays.toString(absN.toBox(man)));
//		
//		
//		
//		Interval[] initialBox = new Interval[env.getVars().length];
//		Interval intervalVar = new Interval(1, 1);
//		int i = 0;
//		for (Var v : env.getVars()) {
//			if (v.compareTo(new StringVar(var)) == 0) {
//				initialBox[i] = intervalVar;
//			}else{
//				initialBox[i] = new Interval(1, -1);
//			}
//			i++;
//		}
		

		
//		
//		System.out.println(Arrays.toString(env.getVars()));
//		System.out.println(Arrays.toString(initialBox));
//		System.out.println(Arrays.toString(interfaceEntry.toBox(man)));
//		System.out.println(Arrays.toString(interfaceEntry2.toBox(man)));
//		System.out.println(Util.printAbstMap("Test", interfaceOut, env, man));
//		Statement n = new Vertex(null);
//		interfaceOut.put(n, interfaceOut.get(n).joinCopy(man, interfaceEntry));
	}
	
	
	
	private static void minimalApronExample() throws ApronException {
		System.out.println("\n\n=========================");
		
		Manager man = new Box();
		String[] varNames = { "k1", "k2" };
		Environment env = new Environment(varNames, new String[] {});
		//Abstract1 xempty = new Abstract1(man, env, true);
		int[] ns = {0, 1, 2};
		Abstract1[] abstractVars= new Abstract1[ns.length];
		
		//initialive env
		for (int n : ns) {
			abstractVars[n] = new Abstract1(man, env, true);
		}
		System.out.println("==set both to [1, -1]==");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractVars[n].toBox(man)));
		}
	
		Interval[] initialBox = new Interval[varNames.length];
		for (int i = 0; i < varNames.length; i++) {
			initialBox[i] = new Interval(0, 0);
		}
		
		//assign [0, 0] to n = 2
		Abstract1 envN0 = new Abstract1(man, env, varNames, initialBox);
		abstractVars[2].join(man, envN0);
		
		//test:
		initialBox[0] = new Interval(0, 3);
		//tset
		Abstract1 envN = new Abstract1(man, env, varNames, initialBox);
		abstractVars[0].join(man, envN);
	
		
		
		
		
		System.out.println("==set k1 to [0, 3], k2 to [0, 0]== set n = 2 to [0, 0]");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		
	
		
		//punkt 0 ist Transition ändert Var. k1: k1 < 3
		
		Texpr1Node xtxpr = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
	    		new Texpr1CstNode(new MpqScalar(3)), new Texpr1VarNode("k1"));
	
		Texpr1Intern xtexp = new Texpr1Intern(env, xtxpr);
		
		Tcons1 xtcons = new Tcons1(Tcons1.SUP, xtexp);
		abstractVars[0].meet(man, xtcons);
		
		System.out.println("==== apply: " + xtcons + " ====");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		//punkt 0 ist Aktion ändert Var. k1: k1 ++
		
		Texpr1Node xtxpr2 = new Texpr1BinNode(Texpr1BinNode.OP_ADD,
	            		new Texpr1VarNode("k1"),new Texpr1CstNode(new MpqScalar(1)));
		Texpr1Intern xtexp2 = new Texpr1Intern(env, xtxpr2);
		abstractVars[0].assign(man, "k1", xtexp2, null);
		
		System.out.println("==== apply: " + xtexp2 + " to k1 ====");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		//punkt 0 ist Transition ändert Var. k1: k1 = 3
	
		Texpr1Node xtxpr3 = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
	    		new Texpr1CstNode(new MpqScalar(3)), new Texpr1VarNode("k1"));
	
		Texpr1Intern xtexp3 = new Texpr1Intern(env, xtxpr3);
		
		Tcons1 xtcons3 = new Tcons1(Tcons1.EQ, xtexp3);
		abstractVars[0].meet(man, xtcons3);
		
		System.out.println("==== apply: " + xtcons3 + " ====" );
		for (int n : ns) {
			System.out.println(Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		//join n = 0 with n = 2
		
		abstractVars[0].join(man, abstractVars[2]);
		
		System.out.println("==== join n = 0 with n = 2 ====" );
		for (int n : ns) {
			System.out.println(Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		//punkt 0 ist Aktion ändert Var. k1: k1 = 0
		Texpr1Node xtxpr4 = new Texpr1CstNode(new MpqScalar(0));
		Texpr1Intern xtexp4 = new Texpr1Intern(env, xtxpr4);
		abstractVars[0].assign(man, "k1", xtexp4, null);
		
		System.out.println("==== apply: " + xtexp4 + " to k1 ====");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		//punkt 0: bedigung für k1 nicht erfüllt (k1 > 10) 
		Texpr1Node xtxpr5 = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
	    		new Texpr1VarNode("k1"), new Texpr1CstNode(new MpqScalar(10)));
		
		Texpr1Intern xtexp5 = new Texpr1Intern(env, xtxpr5);
		
		Tcons1 xtcons5 = new Tcons1(Tcons1.SUP, xtexp5);
		abstractVars[0].meet(man, xtcons5);
		
		
		
		System.out.println("==== apply: " + xtcons5 + " ====");
		System.out.println(xtxpr5.toString() + " -- "+ xtexp5.toString() + " -- " + xtcons5.toString());
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractVars[n].toBox(man)));
		}
		
		
		System.out.println("=========================");
		
		//Test booleans
		
		String[] varNamesAll = {"DI_1", "AI_1", "D_2", "A_2", "DO_1", "AO_1"};
		String[] varNamesInput = {"DI_1", "AI_1"};
		String[] varNamesBool = {"DI_1", "D_2", "DO_1"};
		String[] varNamesInteger = {"AI_1", "A_2", "AO_1"};
		Environment envInteger = new Environment(varNamesInteger, new String[] {});
		Environment envBool = new Environment(varNamesBool, new String[] {});
		Environment envAll = new Environment(varNamesAll, new String[] {});
		//Abstract1 xempty = new Abstract1(man, env, true);
		int[] statements = {0, 1, 2};
		//Abstract1[] abstractIntegerVars= new Abstract1[statements.length];
		//Abstract1[] abstractBoolVars= new Abstract1[statements.length];
		Abstract1[] abstractAllVars= new Abstract1[statements.length];
		Abstract1 xfull = new Abstract1(man, envAll);
		
		//initialive env
		for (int n : statements) {
			//abstractIntegerVars[n] = new Abstract1(man, envInteger, true);
			//abstractBoolVars[n] = new Abstract1(man, envBool, true);
			abstractAllVars[n] = new Abstract1(man, envAll, true);
		}
		System.out.println("==set both to [1, -1]==");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractAllVars[n].toBox(man)));
			//System.out.println( Arrays.toString(abstractBoolVars[n].toBox(man)));
		}
	
		//Interval[] initialBoxInteger = new Interval[varNamesInteger.length];
		//Interval[] initialBoxBool = new Interval[varNamesBool.length];
		Interval[] initialBoxAll= new Interval[varNamesAll.length];
		for (int i = 0; i < varNamesAll.length; i++) {
			initialBoxAll[i] = new Interval(0, 0);
		}
		
		
		abstractAllVars[0].join(man, new Abstract1(man, envAll, varNamesAll, initialBoxAll));
		
		System.out.println("==initialize==");
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractAllVars[n].toBox(man)));
			//System.out.println( Arrays.toString(abstractBoolVars[n].toBox(man)));
		}
		
		
		//Input Vars: TODO wie sollten die nichtdet. input Vars am besten modelliert werden? als Var, oder einfach als Intervall?
	
		
	
		Texpr1Node xprA = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
	    		new Texpr1CstNode(new MpqScalar(2)), new Texpr1VarNode("A_2"));
		Texpr1Intern exprA = new Texpr1Intern(envAll, xprA);
		Tcons1 consA = new Tcons1(Tcons1.SUP, exprA);
		System.out.println(consA.toString());
		
		Texpr1Node xprB = new Texpr1BinNode(Texpr1BinNode.OP_SUB,
	    		new Texpr1VarNode("D_2"), new Texpr1CstNode(new MpqScalar(1)));
		Texpr1Intern exprB = new Texpr1Intern(envAll, xprB);
		Tcons1 consB = new Tcons1(Tcons1.EQ, exprB);
		System.out.println(consB.toString());
		
		
		
		//D2 || A2 > 2
		
		Abstract1 copyN0 = abstractAllVars[0].meetCopy(man, consA);
		copyN0.join(man, abstractAllVars[0].meetCopy(man, consB));
	
		System.out.println("== " + consA + " || " + consB + " ==");
		System.out.println(Arrays.toString(varNamesAll));
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractAllVars[n].toBox(man)));
		}
		
		
		
		//!D2 && A2 > 2
		abstractAllVars[0].meet(man, consA);
		abstractAllVars[0].meet(man, consB);
		
		System.out.println("== " + consA + " && " + consB + " ==");
		System.out.println(Arrays.toString(varNamesAll));
		for (int n : ns) {
			System.out.println( Arrays.toString(abstractAllVars[n].toBox(man)));
		}
		
		MpfrScalar inf = new MpfrScalar();
		MpfrScalar fin = new MpfrScalar();
		inf.setInfty(1);
		fin.setInfty(-1);
		System.out.println(new Texpr1CstNode(new Interval(fin, inf)));
	
	}
	
	
}
