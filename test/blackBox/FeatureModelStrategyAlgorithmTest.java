package blackBoxTesting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fm.Feature;
import fm.FeatureDiagram;
import grl.ActorRef;
import grl.Belief;
import grl.ElementLink;
import grl.Evaluation;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GRLNode;
import grl.GRLspec;
import grl.GrlFactory;
import grl.IntentionalElement;
import grl.IntentionalElementRef;
import grl.StrategiesGroup;
import grl.impl.EvaluationImpl;
import grl.impl.GrlFactoryImpl;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.mcgill.sel.core.COREFeature;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.commands.create.CreateGrlGraphCommand;
import seg.jUCMNav.model.commands.create.CreateMapCommand;
import seg.jUCMNav.model.commands.delete.DeleteMapCommand;
import seg.jUCMNav.model.util.MetadataHelper;
import seg.jUCMNav.model.util.ParentFinder;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.strategies.FeatureModelStrategyAlgorithm;
import seg.jUCMNav.strategies.util.FeatureUtil;
import seg.jUCMNav.views.preferences.DeletePreferences;
import seg.jUCMNav.views.preferences.StrategyEvaluationPreferences;
import ucm.map.UCMmap;
import urn.URNlink;
import urn.URNspec;
import urn.UrnFactory;
import urn.impl.UrnFactoryImpl;
import urncore.IURNDiagram;
import urncore.Metadata;
import static org.junit.Assert.*;

/*
 * 
 * QUESTIONS AND ISSUES:
 * ---Root does not evaluate properly (usually evaluates to 0 instead of 100) and will cause lots of test to fail. Keep assertion or remove?
 * ---There is no "autoselectAllMandatoryParents". If user selects a mandatory child, the parent will not be autoselected and causes failure
 * ---Evaluations for an Invalid Feature Model??
 * 
 */
public class FeatureModelStrategyAlgorithmTest {
	private UCMNavMultiPageEditor editor;
	private CommandStack cs;
	private URNspec urnspec;
	private GRLGraph graph;
	private IntentionalElementRef ref;
	private Belief belief;
	private HashMap eval;
	// private Actor actor;
	private ActorRef actorref;
	private ActorRef actorref2;
	private ElementLink link;
	private StrategiesGroup strategiesgroup;
	private EvaluationStrategy strategy;
	private URNlink urnlink;
	private IntentionalElementRef ieRef1, ieRef2, ieRef3, ieRef4, ieRef5,
			ieRef6, ieRef7, ieRef8, ieRef9, ieRef10, ieRef11, ieRef12, ieRef13,
			ieRef14, ieRef15, ieRef16;
	private ActorRef aRef1, aRef2, aRef3;
	private GRLGraph graph1, graph3;
	private boolean testBindings;
	private FeatureModelStrategyAlgorithm algo;


	public void setUp(String testFileName) throws Exception {
		testBindings = true;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject testproject = workspaceRoot.getProject("jUCMNavTest"); //$NON-NLS-1$
		if (!testproject.exists()) {
			System.out.println("can't find jUCMNavTest");
			testproject.create(null);
		}
		if (!testproject.isOpen()) {
			testproject.open(null);
		}
		IFile testfile = testproject.getFile(testFileName); //$NON-NLS-1$
		// IFile testfile = (IFile) new File("C:\\Users\\Quang\\elevator.jucm");
		// start with clean file
		if (!testfile.exists()) {
			// testfile.delete(true, false, null);
			System.out.println("can't find File");
		}
		//System.out.println("found elevator");
		//testfile.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry()
				.getDefaultEditor(testfile.getName());
		editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(
				testfile), desc.getId());
		UCMNavMultiPageEditor e = new UCMNavMultiPageEditor();
		// generate a top level model element
		urnspec = editor.getModel();
		cs = editor.getDelegatingCommandStack();
		// Delete the default UCM map, if present
		Command cmd;
		Object defaultMap = urnspec.getUrndef().getSpecDiagrams().get(0);
		if (defaultMap instanceof UCMmap) {
			cmd = new DeleteMapCommand((UCMmap) defaultMap);
			assertTrue("Can't execute DeleteMapCommand.", cmd.canExecute()); //$NON-NLS-1$
			cs.execute(cmd);
		}
		// Create a new GRLGraph
		cmd = new CreateGrlGraphCommand(urnspec);
		graph = ((CreateGrlGraphCommand) cmd).getDiagram();
		assertTrue("Can't execute CreateGrlGraphCommand.", cmd.canExecute()); //$NON-NLS-1$
		cs.execute(cmd);
		// Set the preferences for deleting the references to ALWAYS
		DeletePreferences.getPreferenceStore().setValue(
				DeletePreferences.PREF_DELDEFINITION,
				DeletePreferences.PREF_ALWAYS);
		DeletePreferences.getPreferenceStore().setValue(
				DeletePreferences.PREF_DELREFERENCE,
				DeletePreferences.PREF_ALWAYS);
		eval = new HashMap();
	}

	@After
	public void tearDown() throws Exception {
		// super.tearDown();
		eval = null;
		editor.doSave(null);
		// Verify the Actor References binding and executing undo/redo
		if (testBindings) {
			verifyBindings();
		}
		int i = cs.getCommands().length;
		if (cs.getCommands().length > 0) {
			assertTrue("Can't undo first command", cs.canUndo()); //$NON-NLS-1$
			cs.undo();
			editor.doSave(null);
			assertTrue("Can't redo first command", cs.canRedo()); //$NON-NLS-1$
			cs.redo();
			editor.doSave(null);
		}
		while (i-- > 0) {
			assertTrue("Can't undo a certain command", cs.canUndo()); //$NON-NLS-1$
			cs.undo();
		}
		editor.doSave(null);
		i = cs.getCommands().length;
		while (i-- > 0) {
			assertTrue("Can't redo a certain command", cs.canRedo()); //$NON-NLS-1$
			cs.redo();
		}
		if (testBindings) {
			verifyBindings();
		}
		editor.doSave(null);
		editor.closeEditor(false);
	}
	
	//One Feature. Not selected
	@Test
	public void test1() {
		String testFile = "TestCase1-2.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eval2 = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				}
			}		
		}
	}
	
	//One Feature. Selected
	@Test
	public void test2() {
		String testFile = "TestCase1-2.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eval2 = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected." , evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One Mandatory. None Selected (also ALL Mandatory)
	@Test
	public void test3() {
		String testFile = "TestCase3-5.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eva2l = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One Mandatory. One selected (also ALL Mandatory)
	@Test
	public void test4() {
		String testFile = "TestCase3-5.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eva2l = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}

	//Two Features, One Mandatory. Both Selected (also ALL Mandatory)
	@Test
	public void test5() {
		String testFile = "TestCase3-5.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap ev2al = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}

	
	//Multiple Features, all mandatory. None Selected
	@Test
	public void test6() {
		String testFile = "TestCase6-8.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eval2 = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, all mandatory.  3 Selected
	@Test
	public void test7() {
		String testFile = "TestCase6-8.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eval2 = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0 || element.getName().compareTo("Mandatory2") == 0 ||
					element.getName().compareTo("Mandatory3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, all mandatory. All Selected
	@Test
	public void test8() {
		String testFile = "TestCase6-8.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eval2 = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than One Mandatory, One Optional Link: not selected, No AND links NONE SELECTED
	@Test
	public void test9() {
		String testFile = "TestCase9-11.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eval2 = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one mandatory, One optional Feature: Selected (1 selected)
	@Test
	public void test10() {
		String testFile = "TestCase9-11.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eva2l = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one mandatory, One optional Feature: Selected (All selected)
	@Test
	public void test11() {
		String testFile = "TestCase9-11.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap ev2al = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}

	
	//Multiple Features, More than One Mandatory, More than one Optional Link (2): both not selected, No AND links
	@Test
	public void test12() {
		String testFile = "TestCase12-14.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eva2l = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 ||
						element.getName().compareTo("Optional2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than One Mandatory, More than one Optional Link (2): one not selected, one selected, No AND links (1 selected)
	@Test
	public void test13() {
		String testFile = "TestCase12-14.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap eva2l = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than One Mandatory, More than one Optional Link (2): both selected, No AND links (All selected)
	@Test
	public void test14() {
		String testFile = "TestCase12-14.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap ev2al = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One optional link: none selected, No AND links
	@Test
	public void test15() {
		String testFile = "TestCase15-17.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		HashMap ev2al = new HashMap();
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One optional link: optional selected, No AND links
	@Test
	public void test16() {
		String testFile = "TestCase15-17.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One optional link: optional, No AND links All selected
	@Test
	public void test17() {
		String testFile = "TestCase15-17.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, 3 optional links: none selected, No AND links
	@Test
	public void test18() {
		String testFile = "TestCase18-20.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0 || 
						element.getName().compareTo("Optional3") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, 3 optional links: Two Optional selected, No AND links
	@Test
	public void test19() {
		String testFile = "TestCase18-20.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, 3 optional links, No AND links All selected
	@Test
	public void test20() {
		String testFile = "TestCase18-20.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, All optional (4), Non selected
		@Test
	public void test21() {
			String testFile = "TestCase21-23.jucm";
			try {
				setUp(testFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
			}
			
			int evalResult;
			GrlFactory factory = GrlFactoryImpl.init();
			
			strategy = factory.createEvaluationStrategy();
			EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
			Evaluation notSelected = factory.createEvaluation();
			Evaluation selected = factory.createEvaluation();
			notSelected.setEvaluation(0);
			selected.setEvaluation(100);
			
			strategy.setGrlspec(urnspec.getGrlspec());
			
			List<Feature> features = strategy.getGrlspec().getIntElements();
			Iterator it = features.iterator();
			while (it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				eval.put(element, notSelected);
			}
			
			algo = new FeatureModelStrategyAlgorithm();
			algo.clearAllAutoSelectedFeatures(strategy);
			algo.autoSelectAllMandatoryFeatures(strategy);
			algo.init(strategy, eval);
			
			em.setStrategy(strategy);
			
			it = features.iterator();
			while(it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				if (element instanceof Feature) {
					evalResult = algo.getEvaluation(element);
					if (element.getName().compareTo("Optional1") == 0 ||element.getName().compareTo("Optional2") == 0 || 
							element.getName().compareTo("Optional3") == 0 || element.getName().compareTo("Optional4") == 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
					} else {
						if (element.getName().compareTo("Root") != 0) {
							assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
						}					
					}
				}		
			}
		}
		
	//Multiple Features, All optional (4), 2 selected
	@Test
	public void test22() {
		String testFile = "TestCase21-23.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional4") == 0 || element.getName().compareTo("Optional2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") == 0 || element.getName().compareTo("Optional1") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, All optional (4), Selected
	@Test
	public void test23() {
		String testFile = "TestCase21-23.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one optional (3), One AND. None selected
	@Test
	public void test24() {
		String testFile = "TestCase24-26.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				}
			}		
		}
	}
	
	//Multiple Features, More than one optional (3), One AND. 2 selected
	@Test
	public void test25() {
		String testFile = "TestCase24-26.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional2") == 0 || element.getName().compareTo("And1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	///Multiple Features, More than one optional (3), One AND. All selected
	@Test
	public void test26() {
		String testFile = "TestCase24-26.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}

	//Multiple Features, More than one optional (3), More than One AND (3). None selected
	@Test
	public void test27() {
		String testFile = "TestCase27-29.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				}
			}		
		}
	}
	
	//Multiple Features, More than one optional (3), More than One AND (3). 2 selected
	//ERROR: Optional3 Feature Does not appear as selected eventhough user defined
	@Test
	public void test28() {
		String testFile = "TestCase27-29.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("And2") == 0 || element.getName().compareTo("Optional3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional2") == 0 || element.getName().compareTo("Optional1") == 0) {
					System.out.println(element.getName() + ": " + evalResult);
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					System.out.println(element.getName() + ": " + evalResult);
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one optional (3), More than One AND (3). All selected
	@Test
	public void test29() {
		String testFile = "TestCase27-29.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}

	//Multiple Features, One optional (1), More than One AND (3). None selected
	@Test
	public void test30() {
		String testFile = "TestCase30-32.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("And1") == 0 || element.getName().compareTo("And2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One optional (1), More than One AND (3). One selected
	//ERROR Optional1 Feature was not selected even though user defined. ERROR when optional has AND child
	@Test
	public void test31() {
		String testFile = "TestCase30-32.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One optional (1), More than One AND (3. All selected
	@Test
	public void test32() {
		String testFile = "TestCase30-32.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, All AND (4). None selected
	@Test
	public void test33() {
		String testFile = "TestCase33-35.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, All AND (4). One selected
	@Test
	public void test34() {
		String testFile = "TestCase33-35.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("And4") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, All AND (4). All selected
	@Test
	public void test35() {
		String testFile = "TestCase33-35.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one AND (3), More than one Mandatory (2). None selected
	@Test
	public void test36() {
		String testFile = "TestCase36-38.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one AND (3), More than one Mandatory (2). 2 selected
	@Test
	public void test37() {
		String testFile = "TestCase36-38.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory2") == 0 || element.getName().compareTo("And2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one AND (3), More than one Mandatory (2). All selected
	@Test
	public void test38() {
		String testFile = "TestCase36-38.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one AND (3), One Mandatory (1). None selected
	@Test
	public void test39() {
		String testFile = "TestCase39-41.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one AND (3), One Mandatory (1). 2 selected
	@Test
	public void test40() {
		String testFile = "TestCase39-41.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0 || element.getName().compareTo("And1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one AND (3), One Mandatory (1). All selected
	@Test
	public void test41() {
		String testFile = "TestCase39-41.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One AND (1), More than one Mandatory (3). None selected
	@Test
	public void test42() {
		String testFile = "TestCase42-44.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One AND (1), More than one Mandatory (3). 2 selected
	@Test
	public void test43() {
		String testFile = "TestCase42-44.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0 || element.getName().compareTo("Mandatory2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One AND (1), More than one Mandatory (3). All selected
	@Test
	public void test44() {
		String testFile = "TestCase42-44.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One AND (1), One Mandatory (1). None selected
	@Test
	public void test45() {
		String testFile = "TestCase45-47.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One AND (1), One Mandatory (1). 1 selected
	@Test
	public void test46() {
		String testFile = "TestCase45-47.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One AND (1), One Mandatory (1). All selected
	@Test
	public void test47() {
		String testFile = "TestCase45-47.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One Optional (1), And (1). None selected
	@Test
	public void test48() {
		String testFile = "TestCase48-50.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Two Features, One Optional (1), And (1). 1 selected
	@Test
	public void test49() {
		String testFile = "TestCase48-50.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Two Features, One Optional (1), And (1). All selected
	@Test
	public void test50() {
		String testFile = "TestCase48-50.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One Optional link, One AND link. None Selected
	@Test
	public void test51() {
		String testFile = "TestCase51-53.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One Optional link, One AND link. 1 Selected
	@Test
	public void test52() {
		String testFile = "TestCase51-53.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One Optional link, One AND link. All Selected
	@Test
	public void test53() {
		String testFile = "TestCase51-53.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One Optional link, More than one AND link (3). None Selected
	@Test
	public void test54() {
		String testFile = "TestCase54-56.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("And3") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One Optional link, More than one AND link (3). 2 Selected
	@Test
	public void test55() {
		String testFile = "TestCase54-56.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("And3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, One Optional link, More than one AND link (3). All Selected
	@Test
	public void test56() {
		String testFile = "TestCase54-56.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, More than one Optional link (3), One AND link. None Selected
	@Test
	public void test57() {
		String testFile = "TestCase57-59.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, More than one Optional link (3), One AND link. 2 Selected
	@Test
	public void test58() {
		String testFile = "TestCase57-59.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") == 0) {
					System.out.println(element.getName() + ": " + evalResult);
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					System.out.println(element.getName() + ": " + evalResult);
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, More than one Optional link (3), One AND link. All Selected
	@Test
	public void test59() {
		String testFile = "TestCase57-59.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, More than One Optional link (3), More than one AND link (3). None Selected
	@Test
	public void test60() {
		String testFile = "TestCase60-62.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") == 0 || element.getName().compareTo("Optional1") == 0 ||
						element.getName().compareTo("Optional2") == 0 || element.getName().compareTo("And2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, More than One Optional link (3), More than one AND link (3). 1 Selected
	@Test
	public void test61() {
		String testFile = "TestCase60-62.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") == 0 || element.getName().compareTo("Optional2") == 0 || 
						element.getName().compareTo("And2") == 0) {
					System.out.println(element.getName() + ": " + evalResult);
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					System.out.println(element.getName() + ": " + evalResult);
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, One Mandatory, More than One Optional link (3), More than one AND link (3). All Selected
	@Test
	public void test62() {
		String testFile = "TestCase60-62.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory, One Optional link, One AND link. None Selected
	@Test
	public void test63() {
		String testFile = "TestCase63-65.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("And1") == 0 ||
						element.getName().compareTo("Mandatory4") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory (4), One Optional link, One AND link. 1 Selected
	//ERROR: Mandatory child of AND feature does not evaluate correctly it seems
	@Test
	public void test64() {
		String testFile = "TestCase63-65.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("And1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than One Mandatory (4), One Optional link, One AND link. All Selected
	@Test
	public void test65() {
		String testFile = "TestCase63-65.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory (3), One Optional link, More than one AND link (3). None Selected
	@Test
	public void test66() {
		String testFile = "TestCase66-68.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("And1") == 0 ||
						element.getName().compareTo("Mandatory2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory (3), One Optional link, More than one AND link (3). 3 Selected
	@Test
	public void test67() {
		String testFile = "TestCase66-68.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("And1") == 0 || element.getName().compareTo("Mandatory2") == 0 || 
					element.getName().compareTo("And3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory (3), One Optional link, More than one AND link (3). All Selected
	@Test
	public void test68() {
		String testFile = "TestCase66-68.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory(3), More than One Optional link (3), One AND link. None Selected
	@Test
	public void test69() {
		String testFile = "TestCase69-71.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0 ||
						element.getName().compareTo("Optional3") == 0 || element.getName().compareTo("And1") == 0 || 
						element.getName().compareTo("Mandatory2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory(3), More than one Optional link (3), One AND link. 2 Selected
	//ERROR: Selected AND feature does not select parents
	@Test
	public void test70() {
		String testFile = "TestCase69-71.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional2") == 0 || 
					element.getName().compareTo("And1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional3") ==0) {
					System.out.println(element.getName() + ": " + evalResult);
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					System.out.println(element.getName() + ": " + evalResult);
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory(3), More than One Optional link (3), One AND link (3). All Selected
	@Test
	public void test71() {
		String testFile = "TestCase69-71.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory(2), More than One Optional link (2), More than one AND link(3). None Selected
	@Test
	public void test72() {
		String testFile = "TestCase72-74.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0 ||
						element.getName().compareTo("And1") == 0 || element.getName().compareTo("And2") == 0 || 
						element.getName().compareTo("And3") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory(2), More than one Optional link (2), More than one AND link(3). 3 Selected
	//ERROR: Optional1 does not retain user-defined Satisfaction value. Possibly because last one AND feature not selected (should be autoselecting?)
	@Test
	public void test73() {
		String testFile = "TestCase72-74.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("And2") == 0 ||
					element.getName().compareTo("And3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional2") == 0) {
					System.out.println(element.getName() + ": " + evalResult);
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					System.out.println(element.getName() + ": " + evalResult);
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features, More than one Mandatory(3), More than One Optional link (3), One AND link (3). All Selected
	@Test
	public void test74() {
		String testFile = "TestCase72-74.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}

/*
 * 
 * 
 * The following Test Cases are for FM with multiple features and multiple incoming nodes permitted
 * 
 * 
 */
	
	//Multiple Features, All Mandatory, multiple links. None Selected
	@Test
	public void test75() {
		String testFile = "TestCase75-77.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, All Mandatory, multiple links. One Selected
	@Test
	public void test76() {
		String testFile = "TestCase75-77.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features, All Mandatory, All selected. All Selected
	@Test
	public void test77() {
		String testFile = "TestCase75-77.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Mandatory (3), Optional (1). None Selected
	@Test
	public void test78() {
		String testFile = "TestCase78-80.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Mandatory(3), Optional(1). One Selected
	@Test
	public void test79() {
		String testFile = "TestCase78-80.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("MO1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links Mandatory(3) Optional (1). All Selected
	@Test
	public void test80() {
		String testFile = "TestCase78-80.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Mandatory (3), Optional (3). None Selected
	@Test
	public void test81() {
		String testFile = "TestCase81-83.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Mandatory(3), Optional(3). One Selected
	@Test
	public void test82() {
		String testFile = "TestCase81-83.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0 || element.getName().compareTo("Mandatory2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Mandatory(3), Optional(3) All Selected
	@Test
	public void test83() {
		String testFile = "TestCase81-83.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, All optional (3). None Selected
	@Test
	public void test84() {
		String testFile = "TestCase84-86.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				}
			}		
		}
	}
	
	//Multiple Features/Links, All optional. 2 Selected
	@Test
	public void test85() {
		String testFile = "TestCase84-86.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, All Optional. All Selected
	@Test
	public void test86() {
		String testFile = "TestCase84-86.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Optional (3), And (1). None Selected
	@Test
	public void test87() {
		String testFile = "TestCase87-89.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Optional (3), And (1). 1 Selected
	@Test
	public void test88() {
		String testFile = "TestCase87-89.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Optional(3), And(1). All Selected
	@Test
	public void test89() {
		String testFile = "TestCase87-89.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, >1 Optional,  > 1 Decomposition. None Selected
	@Test
	public void test90() {
		String testFile = "TestCase90-92.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				}
			}		
		}
	}
	
	//Multiple Features/Links, >1 Optional , >1 Decomposition. 2 Selected
	//Invalid Feature Model More than one XOR selected
	@Test
	public void test91() {
		String testFile = "TestCase90-92.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("XOrOptional1") == 0 || element.getName().compareTo("XOrOptional2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				//if (element.getName().compareTo("Root") != 0) {
				//	assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				//}
			}		
		}
	}
	
	//Multiple Features/Links, >1 Optional,  >1 Decomposition. All Selected
	//EXPECTED VALUE FOR THIS CASE?
	@Test
	public void test92() {
		String testFile = "TestCase90-92.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links, Optional (1),  > 1 Decomposition. None Selected
	@Test
	public void test93() {
		String testFile = "TestCase93-95.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
			}		
		}
	}
	
	//Multiple Features/Links, Optional(1) , >1 Decomposition. 1 Selected
	//Failure
	@Test
	public void test94() {
		String testFile = "TestCase93-95.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Or2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links,  Optional (1),  >1 Decomposition. All Selected
	@Test
	public void test95() {
		String testFile = "TestCase93-95.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}

	//Multiple Features/Links, All Decomposition. None Selected
	@Test
	public void test96() {
		String testFile = "TestCase96-98.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
			}		
		}
	}
	
	//Multiple Features/Links, All Decomposition. 1 Selected
	//INVALID. EXPECTED VALUE?
	@Test
	public void test97() {
		String testFile = "TestCase96-98.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("XOr3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links,  All Decomposition. All Selected
	//INVALID, EXPECTED VALUE?
	@Test
	public void test98() {
		String testFile = "TestCase96-98.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
//Multiple Features/Links, > 1 Decomposition, 1 Mandatory. None Selected
	@Test
	public void test99() {
		String testFile = "TestCase99-101.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
			}		
		}
	}
	
	//Multiple Features/Links, > 1 Decomposition, 1 Mandatory. 1 Selected
	@Test
	public void test100() {
		String testFile = "TestCase99-101.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Or1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("XOr1") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features/Links,  > 1 Decomposition, 1 Mandatory. All Selected
	@Test
	public void test101() {
		String testFile = "TestCase99-101.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, > 1 Decomposition, > 1 Mandatory. None Selected
	@Test
	public void test102() {
		String testFile = "TestCase102-104.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, > 1 Decomposition, > 1 Mandatory. 2 Selected
	@Test
	public void test103() {
		String testFile = "TestCase102-104.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("And1") == 0 || element.getName().compareTo("And2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links,  > 1 Decomposition, > 1 Mandatory. All Selected
	@Test
	public void test104() {
		String testFile = "TestCase102-104.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Decomposition, > 1 Mandatory. None Selected
	@Test
	public void test105() {
		String testFile = "TestCase105-107.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Decomposition, > 1 Mandatory. 1 Selected
	@Test
	public void test106() {
		String testFile = "TestCase105-107.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links,  1 Decomposition, > 1 Mandatory. All Selected
	@Test
	public void test107() {
		String testFile = "TestCase105-107.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, 1 Optional, >1 Decomposition. None Selected
	@Test
	public void test108() {
		String testFile = "TestCase108-110.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, 1 Optional, >1 Decomposition. 1 Selected
	@Test
	public void test109() {
		String testFile = "TestCase108-110.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("MandatoryOptionalOr1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links,  1 Mandatory, 1 Optional, >1 Decomposition. All Selected
	@Test
	public void test110() {
		String testFile = "TestCase108-110.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, >1 Optional, 1 Decomposition. None Selected
	@Test
	public void test111() {
		String testFile = "TestCase111-113.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, >1 Optional, 1 Decomposition. 1 Selected
	@Test
	public void test112() {
		String testFile = "TestCase111-113.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0 ||
						element.getName().compareTo("ManOpXor") == 0 || element.getName().compareTo("Optional4") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features/Links,  1 Mandatory, >1 Optional, 1 Decomposition. All Selected
	@Test
	public void test113() {
		String testFile = "TestCase111-113.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, >1 Optional, >1 Decomposition. None Selected
	@Test
	public void test114() {
		String testFile = "TestCase114-116.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, >1 Optional, >1 Decomposition. 2 Selected
	//INVALID MODEL
	@Test
	public void test115() {
		String testFile = "TestCase114-116.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("XOr2") == 0 || element.getName().compareTo("Optional2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Or1") == 0 || element.getName().compareTo("Or2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features/Links, 1 Mandatory, >1 Optional, >1 Decomposition. All Selected
	//INVALID MODEL
	//Multiple Features/Links, 1 Mandatory, >1 Optional, >1 Decomposition. All Selected
	@Test
	public void test116() {
		String testFile = "TestCase114-116.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, 1 Optional, 1 Decomposition. None Selected
	//Multiple Features/Links, >1 Mandatory, 1 Optional, 1 Decomposition. None Selected
	@Test
	public void test117() {
		String testFile = "TestCase117-119.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, 1 Optional, 1 Decomposition. 3 Selected
	//Multiple Features/Links, >1 Mandatory, 1 Optional, 1 Decomposition. 1 Selected
	@Test
	public void test118() {
		String testFile = "TestCase117-119.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Mandatory1") == 0 || element.getName().compareTo("Mandatory2") == 0 ||
					element.getName().compareTo("Mandatory4") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + " " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links,  >1 Mandatory, 1 Optional, 1 Decomposition. All Selected
	//Multiple Features/Links,  >1 Mandatory, 1 Optional, 1 Decomposition. All Selected
	@Test
	public void test119() {
		String testFile = "TestCase117-119.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, 1 Optional, >1 Decomposition. None Selected
	//Multiple Features/Links, >1 Mandatory, 1 Optional, >1 Decomposition. None Selected
	@Test
	public void test120() {
		String testFile = "TestCase120-122.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Or1") == 0 || element.getName().compareTo("Or2") == 0 ||
						element.getName().compareTo("ManOpXor") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, 1 Optional, >1 Decomposition. 2 Selected
	//Multiple Features/Links, >1 Mandatory, 1 Optional, >1 Decomposition. 1 Selected
	@Test
	public void test121() {
		String testFile = "TestCase120-122.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Or1") == 0 || element.getName().compareTo("Or2") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Root") != 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
				}
			}		
		}
	}
	
	//Multiple Features/Links,  >1 Mandatory, 1 Optional, >1 Decomposition. All Selected
	//Multiple Features/Links,  >1 Mandatory, 1 Optional, >1 Decomposition. All Selected
	@Test
	public void test122() {
		String testFile = "TestCase120-122.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, >1 Optional, 1 Decomposition. None Selected
	//Multiple Features/Links, >1 Mandatory, >1 Optional, 1 Decomposition. None Selected
	@Test
	public void test123() {
		String testFile = "TestCase123-125.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				System.out.println(element.getName() + ": " + evalResult);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, >1 Optional, 1 Decomposition. 3 Selected
	//Multiple Features/Links,  >1 Mandatory, >1 Optional, 1 Decomposition. 1 Selected
	@Test
	public void test124() {
		String testFile = "TestCase123-125.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0 ||
					element.getName().compareTo("Optional3") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("Optional4") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, >1 Optional, 1 Decomposition. All Selected
	//Multiple Features/Links,  >1 Mandatory, >1 Optional, 1 Decomposition. All Selected
	@Test
	public void test125() {
		String testFile = "TestCase123-125.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, >1 Optional, >1 Decomposition. None Selected
	//Multiple Features/Links,  >1 Mandatory, >1 Optional, >1 Decomposition. None Selected
	@Test
	public void test126() {
		String testFile = "TestCase126-128.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, notSelected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("XOr2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, >1 Optional, >1 Decomposition. 1 Selected
	//Multiple Features/Links, >1 Mandatory, >1 Optional, >1 Decomposition. 1 Selected
	@Test
	public void test127() {
		String testFile = "TestCase126-128.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getName().compareTo("Optional1") == 0) {
				eval.put(element, selected);
			} else {
				eval.put(element, notSelected);
			}
			
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				if (element.getName().compareTo("XOr2") == 0) {
					assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected.", evalResult == 0);
				} else {
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}
			}		
		}
	}
	
	//Multiple Features/Links, >1 Mandatory, >1 Optional, >1 Decomposition. All Selected
	//INVALID MODEL
	//Multiple Features/Links,  >1 Mandatory, >1 Optional, >1 Decomposition. All Selected
	@Test
	public void test128() {
		String testFile = "TestCase126-128.jucm";
		try {
			setUp(testFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		
		int evalResult;
		GrlFactory factory = GrlFactoryImpl.init();
		
		strategy = factory.createEvaluationStrategy();
		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
		Evaluation notSelected = factory.createEvaluation();
		Evaluation selected = factory.createEvaluation();
		notSelected.setEvaluation(0);
		selected.setEvaluation(100);
		
		strategy.setGrlspec(urnspec.getGrlspec());
		
		List<Feature> features = strategy.getGrlspec().getIntElements();
		Iterator it = features.iterator();
		while (it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			eval.put(element, selected);
		}
		
		algo = new FeatureModelStrategyAlgorithm();
		algo.clearAllAutoSelectedFeatures(strategy);
		algo.autoSelectAllMandatoryFeatures(strategy);
		algo.init(strategy, eval);
		
		em.setStrategy(strategy);
		
		it = features.iterator();
		while(it.hasNext()) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature) {
				evalResult = algo.getEvaluation(element);
				assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
			}		
		}
	}
/*		
//	@Test
//	public void IDEAL() {
//		int evalResult;
//		GrlFactory factory = GrlFactoryImpl.init();
//		
//		strategy = factory.createEvaluationStrategy();
//		EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
//		
//		Evaluation notSelected = factory.createEvaluation();
//		Evaluation selected = factory.createEvaluation();
//		notSelected.setEvaluation(0);
//		selected.setEvaluation(100);
//		
//		strategy.setGrlspec(urnspec.getGrlspec());
//		
//		List<Feature> features = strategy.getGrlspec().getIntElements();
//		Iterator it = features.iterator();
//		while (it.hasNext()) {
//			IntentionalElement element = (IntentionalElement) it.next();
//			if (element.getName().compareTo("Elevator") == 0 || element.getName().compareTo("Emergency Phone") == 0 || element.getName().compareTo("Voice Announcements") == 0) {
//				eval.put(element, selected);
//			} else {
//				eval.put(element, notSelected);
//			}
//		}
//		
//		algo = new FeatureModelStrategyAlgorithm();
//		algo.clearAllAutoSelectedFeatures(strategy);
//		algo.autoSelectAllMandatoryFeatures(strategy);
//		algo.init(strategy, eval);
//		
//		em.setStrategy(strategy);
//		
//		it = features.iterator();
//		while(it.hasNext()) {
//			IntentionalElement element = (IntentionalElement) it.next();
//			if (element instanceof Feature) {
//				evalResult = algo.getEvaluation(element);
//				if (element.getName().compareTo("Emergency Phone") == 0 || element.getName().compareTo("Voice Announcements") == 0) {
//					System.out.println(element.getName() + ": " + evalResult);
//					//assertTrue(evalResult == 0);
//				} else {
//					System.out.println(element.getName() + ": " + evalResult);
//					//assertTrue(evalResult == 100);
//				}
//			}		
//		}
//	}
 * 
 * 		//Multiple Features, More than one Mandatory(2), More than One Optional link (2), More than one AND link(3). None Selected
		@Test
		public void test72() {
			String testFile = "TestCase72-74.jucm";
			try {
				setUp(testFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
			}
			
			int evalResult;
			GrlFactory factory = GrlFactoryImpl.init();
			
			strategy = factory.createEvaluationStrategy();
			EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
			Evaluation notSelected = factory.createEvaluation();
			Evaluation selected = factory.createEvaluation();
			notSelected.setEvaluation(0);
			selected.setEvaluation(100);
			
			strategy.setGrlspec(urnspec.getGrlspec());
			
			List<Feature> features = strategy.getGrlspec().getIntElements();
			Iterator it = features.iterator();
			while (it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				eval.put(element, notSelected);
			}
			
			algo = new FeatureModelStrategyAlgorithm();
			algo.clearAllAutoSelectedFeatures(strategy);
			algo.autoSelectAllMandatoryFeatures(strategy);
			algo.init(strategy, eval);
			
			em.setStrategy(strategy);
			
			it = features.iterator();
			while(it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				if (element instanceof Feature) {
					evalResult = algo.getEvaluation(element);
					if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("Optional2") == 0 ||
							element.getName().compareTo("And1") == 0 || element.getName().compareTo("And2") == 0 || 
							element.getName().compareTo("And3") == 0) {
						System.out.println(element.getName() + ": " + evalResult);
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
					} else {
						System.out.println(element.getName() + ": " + evalResult);
						if (element.getName().compareTo("Root") != 0) {
							assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
						}
					}
				}		
			}
		}
		
		//Multiple Features, More than one Mandatory(2), More than one Optional link (2), More than one AND link(3). 3 Selected
		//ERROR: Optional1 does not retain user-defined Satisfaction value. Possibly because last one AND feature not selected (should be autoselecting?)
		@Test
		public void test73() {
			String testFile = "TestCase72-74.jucm";
			try {
				setUp(testFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
			}
			
			int evalResult;
			GrlFactory factory = GrlFactoryImpl.init();
			
			strategy = factory.createEvaluationStrategy();
			EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
			Evaluation notSelected = factory.createEvaluation();
			Evaluation selected = factory.createEvaluation();
			notSelected.setEvaluation(0);
			selected.setEvaluation(100);
			
			strategy.setGrlspec(urnspec.getGrlspec());
			
			List<Feature> features = strategy.getGrlspec().getIntElements();
			Iterator it = features.iterator();
			while (it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				if (element.getName().compareTo("Optional1") == 0 || element.getName().compareTo("And2") == 0 ||
						element.getName().compareTo("And3") == 0) {
					eval.put(element, selected);
				} else {
					eval.put(element, notSelected);
				}
				
			}
			
			algo = new FeatureModelStrategyAlgorithm();
			algo.clearAllAutoSelectedFeatures(strategy);
			algo.autoSelectAllMandatoryFeatures(strategy);
			algo.init(strategy, eval);
			
			em.setStrategy(strategy);
			
			it = features.iterator();
			while(it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				if (element instanceof Feature) {
					evalResult = algo.getEvaluation(element);
					if (element.getName().compareTo("Optional2") == 0) {
						System.out.println(element.getName() + ": " + evalResult);
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 0 was expected." , evalResult == 0);
					} else {
						System.out.println(element.getName() + ": " + evalResult);
						if (element.getName().compareTo("Root") != 0) {
							assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
						}
					}
				}		
			}
		}
		
		//Multiple Features, More than one Mandatory(3), More than One Optional link (3), One AND link (3). All Selected
		@Test
		public void test74() {
			String testFile = "TestCase72-74.jucm";
			try {
				setUp(testFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
			}
			
			int evalResult;
			GrlFactory factory = GrlFactoryImpl.init();
			
			strategy = factory.createEvaluationStrategy();
			EvaluationStrategyManager em = EvaluationStrategyManager.getInstance(editor);
			Evaluation notSelected = factory.createEvaluation();
			Evaluation selected = factory.createEvaluation();
			notSelected.setEvaluation(0);
			selected.setEvaluation(100);
			
			strategy.setGrlspec(urnspec.getGrlspec());
			
			List<Feature> features = strategy.getGrlspec().getIntElements();
			Iterator it = features.iterator();
			while (it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				eval.put(element, selected);
			}
			
			algo = new FeatureModelStrategyAlgorithm();
			algo.clearAllAutoSelectedFeatures(strategy);
			algo.autoSelectAllMandatoryFeatures(strategy);
			algo.init(strategy, eval);
			
			em.setStrategy(strategy);
			
			it = features.iterator();
			while(it.hasNext()) {
				IntentionalElement element = (IntentionalElement) it.next();
				if (element instanceof Feature) {
					evalResult = algo.getEvaluation(element);
					System.out.println(element.getName() + ": " + evalResult);
					if (element.getName().compareTo("Root") != 0) {
						assertTrue(element.getName() + " evaluated to " + evalResult + " when 100 was expected.", evalResult == 100);
					}
				}		
			}
		}
 */
	
	public void verifyBindings() {
		for (Iterator iter = urnspec.getUrndef().getSpecDiagrams().iterator(); iter
				.hasNext();) {
			IURNDiagram g = (IURNDiagram) iter.next();
			if (g instanceof GRLGraph) {
				GRLGraph graph = (GRLGraph) g;
				for (Iterator iter2 = graph.getContRefs().iterator(); iter2
						.hasNext();) {
					ActorRef actor = (ActorRef) iter2.next();
					assertEquals(
							"ActorRef " + actor.toString() + " is not properly bound.", ParentFinder.getPossibleParent(actor), actor.getParent()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				for (Iterator iter2 = graph.getNodes().iterator(); iter2
						.hasNext();) {
					GRLNode gn = (GRLNode) iter2.next();
					assertEquals(
							"GRLNode " + gn.toString() + " is not properly bound.", ParentFinder.getPossibleParent(gn), gn.getContRef()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}
}
