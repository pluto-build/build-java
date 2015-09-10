package build.pluto.buildjava.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import build.pluto.buildjava.test.simple.CyclicFilesTest;
import build.pluto.buildjava.test.simple.MultipleFilesTest;
import build.pluto.buildjava.test.simple.SimpleJavaBuildTest;
import build.pluto.buildjava.test.simple.SimultaneousFilesTest;

@RunWith(Suite.class)
@SuiteClasses({ SimpleJavaBuildTest.class, MultipleFilesTest.class, CyclicFilesTest.class, SimultaneousFilesTest.class })
public class BuildJavaTestSuite {

}
