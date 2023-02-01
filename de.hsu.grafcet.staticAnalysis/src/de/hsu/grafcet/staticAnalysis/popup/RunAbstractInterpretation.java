package de.hsu.grafcet.staticAnalysis.popup;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.ecore.resource.ResourceSet;
//import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import apron.ApronException;
import de.hsu.grafcet.*;
import de.hsu.grafcet.staticAnalysis.analysis.AbstractInterpreter;
import de.hsu.grafcet.staticAnalysis.analysis.DeadlockDetecter;
import de.hsu.grafcet.staticAnalysis.analysis.StructuralConcurrencyAnalyzer;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergrafTransromation.HypergrafcetGenerator;

public class RunAbstractInterpretation implements IObjectActionDelegate{

	private Shell shell;
	protected List<IFile> files;	
	private Grafcet selectedGrafcet;
	private int analysis;
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction arg0) {
		if (files != null) {
			try {
				analysis = MessageDialog.open(MessageDialog.QUESTION, shell, 
						"Choose properties to analyze.", 
						"Message", 0, "", "2", "3", "4", "5", "6");
			} catch (Exception e) {
				e.printStackTrace();
			}

			
			IRunnableWithProgress operation = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						Iterator<IFile> filesIt = files.iterator();
						while (filesIt.hasNext()) {
													
							IFile model = (IFile)filesIt.next();
							URI modelURI = URI.createPlatformResourceURI(model.getFullPath().toString(), true);
							try {
								System.out.println(analysis);
								
								IContainer target = model.getProject().getFolder("Static_Analysis");
								ResourceSet resSet = new ResourceSetImpl();
								Resource res = resSet.getResource(modelURI,	true);
								
								String out = "Concurrency Analysis: \n";
								HypergrafcetGenerator hg = new HypergrafcetGenerator((Grafcet)res.getContents().get(0));
								HierarchyOrder hierarchyOrder = hg.returnHierarchyOrder();
								for (HierarchyDependency dependency : hierarchyOrder.getDependencies()) {
									out += "Partial Grafcet " + dependency.getInferior() + "\n Dependency: ";
									out += StructuralConcurrencyAnalyzer.reachabilityConcurrencyAnalysis(dependency, false, true);
									//FIXME: analyze if edges are parallel and add to heirarchy order
								}
								Util.createOutputFile(out, target.getLocation().toString() + "/Result_Concurrency_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
								
								out = "\n\n Analysis cycles abstract Interpretation: \n";
								AbstractInterpreter ai = new AbstractInterpreter(hierarchyOrder.getHypergraf());
								out += ai.sequentialAbstractInterpretation();
								Util.createOutputFile(out, target.getLocation().toString() + "/Result_AI_raw_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
								
								
								out = "\n\n Verification results abstract Interpretation: \n";
								//TODO call methods for properties like Deadlocks
								out += DeadlockDetecter.checkDeadlocks(ai);
								
								Util.createOutputFile(out, target.getLocation().toString() + "/Result_AI_veri_" + model.getName().substring(0, model.getName().lastIndexOf(".grafcet")) + ".txt");
					
								
							} catch (ApronException e){
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}finally {
								model.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
							}
						}
					} catch (CoreException e) {
						IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
						Activator.getDefault().getLog().log(status);
					}
				}
			};
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, operation);
			} catch (InvocationTargetException e) {
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				Activator.getDefault().getLog().log(status);
			} catch (InterruptedException e) {
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				Activator.getDefault().getLog().log(status);
			}
		}
		MessageDialog.openInformation(
				shell,
				"Actions",
				"Checking was executed.");
		
	}
	
	
	

	
	

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@SuppressWarnings("unchecked")
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			files = ((IStructuredSelection) selection).toList();
		}
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	
	
}
