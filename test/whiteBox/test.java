package whiteBox;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import grl.ActorRef;
import grl.Belief;
import grl.ElementLink;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GRLNode;
import grl.IntentionalElementRef;
import grl.StrategiesGroup;

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

import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.commands.create.CreateGrlGraphCommand;
import seg.jUCMNav.model.commands.delete.DeleteMapCommand;
import seg.jUCMNav.model.util.ParentFinder;
import seg.jUCMNav.strategies.FeatureModelStrategyAlgorithm;
import seg.jUCMNav.views.preferences.DeletePreferences;
import ucm.map.UCMmap;
import urn.URNlink;
import urn.URNspec;
import urncore.IURNDiagram;
import junit.framework.TestCase;

public class test extends TestCase {
	private UCMNavMultiPageEditor editor;
    private CommandStack cs;

    private URNspec urnspec;
    private GRLGraph graph;
    private IntentionalElementRef ref;
    private Belief belief;
    // private Actor actor;
    private ActorRef actorref;
    private ActorRef actorref2;
    private ElementLink link;
    private StrategiesGroup strategiesgroup;
    private EvaluationStrategy strategy;
    private URNlink urnlink;
    
    private IntentionalElementRef ieRef1, ieRef2, ieRef3, ieRef4, ieRef5, ieRef6, ieRef7, 
        ieRef8, ieRef9, ieRef10, ieRef11, ieRef12, ieRef13, ieRef14, ieRef15, ieRef16;
    private ActorRef aRef1, aRef2, aRef3;
    private GRLGraph graph1, graph3;

    private boolean testBindings;
    
	private FeatureModelStrategyAlgorithm algo;
	
	public test(String name) {
		super(name);
	}

	@Before
	protected void setUp() throws Exception {
		super.setUp();

        testBindings = true;
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject testproject = workspaceRoot.getProject("jUCMNav-GRL-tests"); //$NON-NLS-1$
        if (!testproject.exists())
            testproject.create(null);

        if (!testproject.isOpen())
            testproject.open(null);

        IFile testfile = testproject.getFile("jUCMNav-GRL-test.jucm"); //$NON-NLS-1$

        // start with clean file
        if (testfile.exists())
            testfile.delete(true, false, null);

        testfile.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
        editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(testfile), desc.getId());
        UCMNavMultiPageEditor e = new UCMNavMultiPageEditor();
        // generate a top level model element
        urnspec = editor.getModel();

        // cs = new CommandStack();
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
        DeletePreferences.getPreferenceStore().setValue(DeletePreferences.PREF_DELDEFINITION, DeletePreferences.PREF_ALWAYS);
        DeletePreferences.getPreferenceStore().setValue(DeletePreferences.PREF_DELREFERENCE, DeletePreferences.PREF_ALWAYS);
	}

	@After
	protected void tearDown() throws Exception {
		super.tearDown();

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

	@Test
	public void test1() {
		algo = new FeatureModelStrategyAlgorithm();
		algo.autoSelectAllMandatoryFeatures(strategy);
	}

	
    public void verifyBindings() {
        for (Iterator iter = urnspec.getUrndef().getSpecDiagrams().iterator(); iter.hasNext();) {
            IURNDiagram g = (IURNDiagram) iter.next();
            if (g instanceof GRLGraph) {
                GRLGraph graph = (GRLGraph) g;

                for (Iterator iter2 = graph.getContRefs().iterator(); iter2.hasNext();) {
                    ActorRef actor = (ActorRef) iter2.next();
                    assertEquals("ActorRef " + actor.toString() + " is not properly bound.", ParentFinder.getPossibleParent(actor), actor.getParent()); //$NON-NLS-1$ //$NON-NLS-2$

                }
                for (Iterator iter2 = graph.getNodes().iterator(); iter2.hasNext();) {
                    GRLNode gn = (GRLNode) iter2.next();
                    assertEquals("GRLNode " + gn.toString() + " is not properly bound.", ParentFinder.getPossibleParent(gn), gn.getContRef()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
    }
}
