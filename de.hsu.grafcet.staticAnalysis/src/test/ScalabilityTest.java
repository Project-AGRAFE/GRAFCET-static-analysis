package test;

import apron.ApronException;
import de.hsu.grafcet.Grafcet;
import de.hsu.grafcet.instanceGeneration.GrafcetSerializer;
import de.hsu.grafcet.instanceGeneration.GrafcetSerializer.GrafcetSerializerInstanceType;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.popup.Util;

public class ScalabilityTest {

	public static void testScalabilityBasicParallel() throws ApronException {
			
		for(int j = 0; j <= 8 ; j ++) {
			int m = j;
			int n = 2;
			long preStart = System.currentTimeMillis();
			GrafcetSerializer serializer = new GrafcetSerializer(GrafcetSerializerInstanceType.BASIC_PARALLEL, m , n);
			Grafcet g = serializer.getGrafcet();
			
			long start = System.currentTimeMillis();
			HierarchyOrder hierarchyOrder = new HierarchyOrder(g);
			long generation = System.currentTimeMillis();
				
			hierarchyOrder.runAbstractInterpretation();
			long end = System.currentTimeMillis();
			String out2 = hierarchyOrder.getAIResult();
//			System.out.println(out2);
			
			long time = end - generation;
			long timeTrans= generation - start;
			long timeInstance = start - preStart;
			System.out.println("m: " + m + " ; time in ms: " + time);
			System.out.println("instantiation: " + timeInstance + " ; transformation: " + timeTrans);
			
		}
		
	}
	
	public static void testScalabilityBasicSequential() throws ApronException {
		
		//Do some dummy stuff:
		GrafcetSerializer serializerd = new GrafcetSerializer(GrafcetSerializerInstanceType.BASIC_SEQUENCE, 300 , 2);
		Grafcet gd = serializerd.getGrafcet();
		HierarchyOrder hierarchyOrderd = new HierarchyOrder(gd);
		hierarchyOrderd.runAbstractInterpretation();
		
		
		for(int j = 4; j <= 8192 ; j = j*2) {
			int m = j;
			int n = 2;
			long preStart = System.currentTimeMillis();
			GrafcetSerializer serializer = new GrafcetSerializer(GrafcetSerializerInstanceType.BASIC_SEQUENCE, m , n);
			Grafcet g = serializer.getGrafcet();
			
			long start = System.currentTimeMillis();
			HierarchyOrder hierarchyOrder = new HierarchyOrder(g);
			long generation = System.currentTimeMillis();
				
			hierarchyOrder.runAbstractInterpretation();
			long end = System.currentTimeMillis();
			String out2 = hierarchyOrder.getAIResult();
//			System.out.println(out2);
			
			long time = end - generation;
			long timeTrans= generation - start;
			long timeInstance = start - preStart;
			System.out.println("m: " + m + " ; time in ms: " + time);
			System.out.println("instantiation: " + timeInstance + " ; transformation: " + timeTrans);
			
		}
		
	}
	
public static void testScalabilityBasicVariable() throws ApronException {
		
		//Do some dummy stuff:
		GrafcetSerializer serializerd = new GrafcetSerializer(GrafcetSerializerInstanceType.BASIC_SEQUENCE, 300 , 2);
		Grafcet gd = serializerd.getGrafcet();
		HierarchyOrder hierarchyOrderd = new HierarchyOrder(gd);
		hierarchyOrderd.runAbstractInterpretation();
		
		
		for(int j = 600; j <= 4800 ; j = j*2) {
			int m = j;
			int n = 5;
			long preStart = System.currentTimeMillis();
			GrafcetSerializer serializer = new GrafcetSerializer(GrafcetSerializerInstanceType.BASIC_VARIABLES, m , n);
			Grafcet g = serializer.getGrafcet();
			
			long start = System.currentTimeMillis();
			HierarchyOrder hierarchyOrder = new HierarchyOrder(g);
			long generation = System.currentTimeMillis();
				
			hierarchyOrder.runAbstractInterpretation();
			long end = System.currentTimeMillis();
			String out2 = hierarchyOrder.getAIResult();
//			System.out.println(out2);
			
			long time = end - generation;
			long timeTrans= generation - start;
			long timeInstance = start - preStart;
			System.out.println("m: " + m + " ; time in ms: " + time);
			System.out.println("instantiation: " + timeInstance + " ; transformation: " + timeTrans);
			
		}
		
	}
}
