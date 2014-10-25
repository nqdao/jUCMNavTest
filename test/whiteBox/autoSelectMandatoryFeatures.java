package whiteBox;

import static org.junit.Assert.*;
import fm.FeatureModel;
import grl.EvaluationStrategy;
import grl.GrlFactory;
import grl.impl.EvaluationStrategyImpl;
import grl.impl.GRLspecImpl;
import grl.impl.GrlFactoryImpl;
import grl.GRLspec;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import seg.jUCMNav.strategies.FeatureModelStrategyAlgorithm;


public class autoSelectMandatoryFeatures {
	EvaluationStrategy strategy;
	FeatureModelStrategyAlgorithm algo;
	
	public autoSelectMandatoryFeatures() {
	}

	@Before
	public void setUp() throws Exception {
		GrlFactory factory = GrlFactoryImpl.init();
		strategy = factory.createEvaluationStrategy();
		GRLspec spec = factory.createGRLspec();
		strategy.setGrlspec(spec);
		algo = new FeatureModelStrategyAlgorithm();
	}

	@After
	public void tearDown() throws Exception {
		algo = null;
		strategy = null;
	}

	@Test
	public void test1() {
		algo.autoSelectAllMandatoryFeatures(strategy);
	}
	
	@Test
	public void test2() {
		try {
			algo.autoSelectAllMandatoryFeatures(null);
		} catch (NullPointerException e) {
			fail(e.getClass().getName());
		}
	}

}
