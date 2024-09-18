package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import apron.Environment;
import apron.Interval;
import apron.Manager;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.CustomArrayDeque;
import terms.VariableDeclaration;

public class ConcurrAbstractInterpreter {
	
	Map<Statement, Abstract1> interferenceStatementAbsEnvMap = new HashMap<Statement, Abstract1>();
	Map<Statement, Abstract1> abstractEnvMap = new HashMap<Statement, Abstract1>(); //abstractEnv for every statement
	
	Manager man = new Box();
	Environment env;
	
	
	CustomArrayDeque<Statement> worklist = new CustomArrayDeque<Statement>();
	HierarchyDependency dependency;
	Subgraf subgraf;
	Set<Statement> statements;
	
	Abstract1 interferenceInPG;
	
	int wideningThreshold = 10;
	

	public ConcurrAbstractInterpreter(HierarchyDependency dependency, Environment env) throws ApronException {
		this.dependency = dependency;
		this.subgraf = dependency.getInferior();
		this.env = env;
		
		statements = subgraf.getStatements();
		subgraf.resetStatementVisitCounter();
		abstractEnvMap = initializeAbstractEnvMap(subgraf);
	}
	
	public ConcurrAbstractInterpreter(HierarchyDependency dependency, Environment env, Abstract1 interferenceInPG) throws ApronException {
		this(dependency, env);
		this.interferenceInPG = interferenceInPG;
	}
	
	private void concurrentAbstracInterpretation() throws ApronException {
		//initialize worklist
 		worklist.addAll(dependency.getInitiallyActiveVertices());
		while (!worklist.isEmpty()) {
			Statement n = worklist.pollFirst();
			n.increaseVisited();
			Abstract1 interferenceInStep = new Abstract1(man, env, true);
			if (n instanceof Vertex) {
				interferenceInStep = calculateInterferenceInStep((Vertex)n);
				joinOrWidening(n, interferenceInStep);			//note: added 20241009
				TransferFunction transferTemp = new TransferFunction(n, abstractEnvMap.get(n), man, env, 
						interferenceInStep.joinCopy(man, interferenceInPG));
				transferTemp.transferInterface();
				Abstract1 interferenceOutN = transferTemp.getInterferenceOutN();
				if (interferenceOutN != null) {
					setInterferenceEntry(interferenceOutN, n);			//if n writes output variable
					if (!interferenceOutN.isBottom(man)) {			//n writes output variable
						for (Vertex vertexConcurrentToN : dependency.getConcurrentVertices((Vertex) n)) {
							if (!abstractEnvMap.get(vertexConcurrentToN).isBottom(man)) {		//add to worklist only if already reachable
								if(!interferenceOutN.isIncluded(man, abstractEnvMap.get(vertexConcurrentToN))) {
									worklist.add(vertexConcurrentToN);
								}
							}
						}
					}							
				}
			}
			TransferFunction transfer = new TransferFunction(n, abstractEnvMap.get(n), man, env, interferenceInStep.joinCopy(man, interferenceInPG));
			Abstract1 e = transfer.transferInterface().joinCopy(man, interferenceInStep.joinCopy(man, interferenceInPG));
			for (Statement nDown : subgraf.getDownstream(n)) {
					if(nDown instanceof Edge) {
						Abstract1 eMeetNeighbourN = new Abstract1(man, e);				//meet of all incoming path for nDown (inclucing path from n)
						for (Vertex neighbourN : ((Edge)nDown).getUpstream()) {
							Abstract1 eNeighbour = transferWithInterface(neighbourN);							
							eMeetNeighbourN.meet(man, eNeighbour);
						}
						if (!eMeetNeighbourN.isIncluded(man, abstractEnvMap.get(nDown))) {
							joinOrWidening(nDown, eMeetNeighbourN);
							worklist.add(nDown);
						}
					} else if (nDown instanceof Vertex) {
						if (!e.isIncluded(man, abstractEnvMap.get(nDown))) {
							joinOrWidening(nDown, e);
							worklist.add(nDown);
						}
						
					}
			}
//			System.out.println("#########n: " + n  + Util.printAbst("Env(n)", abstractEnvMap.get(n), env, man) 
//				+ Util.printAbst("e", e, env, man));
		}
	}
	
	/**
	 * Applies transfer function to Vertex n and its abstract environment Env(n) and returns abstract environment e.
	 * It considers the interference to n before and after apllying the transfer function
	 * @param n	Vertex to be executed in the abstract domain
	 * @return	abstract environment values after n was executed in the abstract domain
	 * @throws ApronException
	 */
	private Abstract1 transferWithInterface(Vertex n) throws ApronException {
		Abstract1 abstractEnvN = abstractEnvMap.get(n);
		Abstract1 interferenceN = calculateInterferenceInStep(n);
		TransferFunction transferN = new TransferFunction(n, abstractEnvN , man, env, interferenceN.joinCopy(man, interferenceInPG));
		Abstract1 e = transferN.transferInterface();
		if (!e.isBottom(man)) {						//ensure to consider the interface only iff n is reachable (e.g. e =! bot)
			e = e.joinCopy(man, interferenceN);
		}
		return e;
	}
	
	/**
	 * widening to ensure termination. If a statement is visited five times the widening operator instead of the join operator is called
	 * @param n statement which influence is calculated in the abstract domain
	 * @param e	abstract environment after n is executed in the abstract domain
	 * @throws ApronException
	 */
	private void joinOrWidening(Statement n, Abstract1 e) throws ApronException {
		if(n.getVisited() > wideningThreshold && !e.isBottom(man)) {
			abstractEnvMap.get(n).join(man, abstractEnvMap.get(n).widening(man, e));
			//reset other widening-counters to make analysis more precise:
			for (Statement s : statements) {
				s.resetVisited();
			}
		} else {
			abstractEnvMap.get(n).join(man, e);
		}
	}
	
	/**
	 * calculation of interference that is influencing n 
	 * @param n vertex that is influenced by the interference
	 * @return a meet value of all interference values of the concurrent vertices of n except n itself
	 * @throws ApronException
	 */
	private Abstract1 calculateInterferenceInStep(Vertex n) throws ApronException {
		Abstract1 interferenceN = new Abstract1(man, env, true);
		Set<Vertex> concurrentVerticesN = dependency.getConcurrentVertices(n);
		for (Statement s : interferenceStatementAbsEnvMap.keySet()) {
			boolean sIsConcurrent = false;
			if (concurrentVerticesN != null) {
				sIsConcurrent = concurrentVerticesN.contains(s);
			}
			if (!s.equals(n) && sIsConcurrent) {
				interferenceN.join(man, interferenceStatementAbsEnvMap.get(s));
			}
		}
		
		return interferenceN;
	}
	
	private Map<Statement, Abstract1>  initializeAbstractEnvMap(Subgraf subgraf) throws ApronException{
		Map<Statement, Abstract1> abstractEnvMap = new HashMap<Statement, Abstract1>();
		//set abstractEnvMap to bottom
		for (Statement n : subgraf.getStatements()) {
			Abstract1 abstractEnvN = new Abstract1(man, env, true);
			abstractEnvMap.put(n, abstractEnvN);
		}
		//set abstractEnv(n) for entry statement to [0, 0]
		Interval[] initialIntervals = new Interval[env.getVars().length];
		for (Statement n : dependency.getInitiallyActiveVertices()) {
			for (int i = 0; i < initialIntervals.length; i++) {
				initialIntervals[i] = new Interval(0, 0);
			}
			Abstract1 abstractEnvN = new Abstract1(man, env, env.getVars(), initialIntervals);
			abstractEnvMap.get(n).join(man, abstractEnvN);
		}
		return abstractEnvMap;
	}
	
	private void setInterferenceEntry(Abstract1 interfaceOutN, Statement n) throws ApronException {
		if (interfaceOutN != null) {
			if (interferenceStatementAbsEnvMap.containsKey(n)) {	//Es müsste reichten, wenn Wert überschriben wird. Informaitonen müssten von Transfer Function enthalten bleiben
				interferenceStatementAbsEnvMap.put(n, interferenceStatementAbsEnvMap.get(n).joinCopy(man, interfaceOutN));
			} else {
				interferenceStatementAbsEnvMap.put(n, interfaceOutN);
			}
		}
	}
	
	public Map<Statement, Abstract1> getInterferenceOut(){
		return interferenceStatementAbsEnvMap;
	}
	
	public Map<Statement, Abstract1> getDeepcopyAbstractEnvMap() throws ApronException {
		return Util.deepcopyEnvMap(man, abstractEnvMap);
	}
	
	public void runAnalysis() throws ApronException {
		concurrentAbstracInterpretation();
	}
	

	public String getResult() throws ApronException {
		Map<Statement, Abstract1> resultAbstractEnvMap = new HashMap<Statement, Abstract1>();
		for (Statement n : abstractEnvMap.keySet()) {
			Abstract1 resultAbstractEnvN = new Abstract1(man, abstractEnvMap.get(n));
			if (n instanceof Vertex) {
				resultAbstractEnvN.join(man, calculateInterferenceInStep((Vertex)n));
			}
			resultAbstractEnvMap.put(n, resultAbstractEnvN);
		}
		return Util.printAbstMap("final result of concurrent abstract interpretation", resultAbstractEnvMap, env, man) + 
				Util.printAbstMap("result of abstractEnvMap", abstractEnvMap, env, man);
	}
	
	

}
