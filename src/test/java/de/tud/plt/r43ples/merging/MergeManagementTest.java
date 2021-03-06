/**
 * 
 */
package de.tud.plt.r43ples.merging;

import de.tud.plt.r43ples.dataset.DataSetGenerationResult;
import de.tud.plt.r43ples.dataset.SampleDataSet;
import de.tud.plt.r43ples.core.HeaderInformation;
import de.tud.plt.r43ples.exception.InternalErrorException;
import de.tud.plt.r43ples.existentobjects.Path;
import de.tud.plt.r43ples.existentobjects.Revision;
import de.tud.plt.r43ples.existentobjects.RevisionGraph;
import de.tud.plt.r43ples.management.Config;
import de.tud.plt.r43ples.optimization.PathCalculationInterface;
import de.tud.plt.r43ples.optimization.PathCalculationSingleton;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;

import static org.hamcrest.core.StringContains.containsString;


/**
 * @author Markus Graube
 *
 */
public class MergeManagementTest {

	/** The data set generation result. **/
	private static DataSetGenerationResult ds;
	/** The revision graph. **/
	private static RevisionGraph graph;
	/** The path calculation interface. **/
	private static PathCalculationInterface pathCalculationInterface;
	
	
	/**
	 * @throws ConfigurationException 
	 * @throws InternalErrorException 
	 * @throws IOException 
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws ConfigurationException, InternalErrorException, IOException {
		Config.readConfig("r43ples.test.conf");
		ds = SampleDataSet.createSampleDataSetMerging();
		graph = new RevisionGraph(ds.graphName);
		pathCalculationInterface = PathCalculationSingleton.getInstance();
	}


	/**
	 * Test method for //{@link de.tud.plt.r43ples.optimization.SimplePathCalculation#getCommonRevisionWithShortestPath(RevisionGraph, Revision, Revision)} .
	 */
	@Test
	public final void testGetCommonRevisionWithShortestPath() throws InternalErrorException {
		Revision commonRevision = pathCalculationInterface.getCommonRevisionWithShortestPath(
				graph,
				new Revision(graph, ds.graphName+"-revision-"+ds.revisions.get("b1-1"), false),
				new Revision(graph, ds.graphName+"-revision-"+ds.revisions.get("b2-2"), false));
		Assert.assertEquals(ds.graphName+"-revision-"+ds.revisions.get("master-1"), commonRevision.getRevisionURI());
	}


	/**
	 * Test method for {@link de.tud.plt.r43ples.optimization.SimplePathCalculation#getPathBetweenStartAndTargetRevision(RevisionGraph, Revision, Revision)} .
	 */
	@Test
	public final void testGetPathBetweenStartAndTargetRevision() throws InternalErrorException {
		Path path = pathCalculationInterface.getPathBetweenStartAndTargetRevision(
				graph,
				new Revision(graph, ds.graphName+"-revision-"+ds.revisions.get("master-0"), false),
				new Revision(graph, ds.graphName+"-revision-"+ds.revisions.get("b2-1"), false));
		LinkedList<String> expected = new LinkedList<>();
        // TODO Create method which can compare LinkedList<String> with Path
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("master-0"));
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("master-1"));
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("b2-0"));
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("b2-1"));
		Assert.assertEquals(expected, path);
	}


	/**
	 * Test method for {@link de.tud.plt.r43ples.optimization.SimplePathCalculation#getPathBetweenStartAndTargetRevision(RevisionGraph, Revision, Revision)} .
	 */
	@Test
	public final void testGetPathBetweenStartAndTargetRevision2() throws InternalErrorException {
		Path path = pathCalculationInterface.getPathBetweenStartAndTargetRevision(
				graph,
                new Revision(graph, ds.graphName+"-revision-"+ds.revisions.get("master-1"), false),
                new Revision(graph, ds.graphName+"-revision-"+ds.revisions.get("b1-1"), false));
		LinkedList<String> expected = new LinkedList<>();
		// TODO Create method which can compare LinkedList<String> with Path
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("master-1"));
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("b1-0"));
		expected.add(ds.graphName+"-revision-"+ds.revisions.get("b1-1"));
		Assert.assertEquals(expected, path);
	}


	@Test
	public void testResponseHeader() {
		String sparql = "SELECT * "
				+ "FROM <"+ds.graphName+">"
				+ "WHERE { ?s ?p ?o}";

		HeaderInformation hi = new HeaderInformation();
		String result = hi.getResponseHeaderFromQuery(sparql);
		Assert.assertThat(result, containsString("Master"));
	}

}
