package de.tud.plt.r43ples.core;

import de.tud.plt.r43ples.exception.InternalErrorException;
import de.tud.plt.r43ples.existentobjects.Branch;
import de.tud.plt.r43ples.existentobjects.FastForwardMergeCommit;
import de.tud.plt.r43ples.existentobjects.Path;
import de.tud.plt.r43ples.existentobjects.Revision;
import de.tud.plt.r43ples.management.Config;
import de.tud.plt.r43ples.management.RevisionManagementOriginal;
import de.tud.plt.r43ples.optimization.PathCalculationInterface;
import de.tud.plt.r43ples.optimization.PathCalculationSingleton;
import de.tud.plt.r43ples.triplestoreInterface.TripleStoreInterfaceSingleton;
import org.apache.log4j.Logger;

import java.util.Iterator;

/**
 * Collection of information for creating a new fast forward merge commit.
 *
 * @author Stephan Hensel
 */
public class FastForwardMergeCommitDraft extends MergeCommitDraft {

    /** The logger. **/
    private Logger logger = Logger.getLogger(FastForwardMergeCommitDraft.class);

    //Dependencies
    /** The path calculation interface to use. **/
    private PathCalculationInterface pathCalculationInterface;


    /**
     * The constructor.
     * Creates a fast forward merge commit draft by using the corresponding meta information.
     *
     * @param graphName the graph name
     * @param branchNameFrom the branch name (from)
     * @param branchNameInto the branch name (into)
     * @param user the user
     * @param message the message
     * @param sdd the SDD URI to use
     * @param triples the triples of the query WITH part
     * @param type the query type (FORCE, AUTO, MANUAL)
     * @param with states if the WITH part is available
     * @throws InternalErrorException
     */
    protected FastForwardMergeCommitDraft(String graphName, String branchNameFrom, String branchNameInto, String user, String message, String sdd, String triples, MergeTypes type, boolean with) throws InternalErrorException {
        super(graphName, branchNameFrom, branchNameInto, user, message, sdd, MergeActions.MERGE, triples, type, with);
        // Dependencies
        this.pathCalculationInterface = PathCalculationSingleton.getInstance();
    }

    /**
     * Tries to create a new commit draft as a new commit in the triple store.
     * If possible it will create the corresponding revision and the meta data.
     *
     * @return the commit (has attribute which indicates if the commit was executed or not)
     */
    protected FastForwardMergeCommit createCommitInTripleStore() throws InternalErrorException {
        String revisionUriFrom = getRevisionGraph().getRevisionUri(getBranchNameFrom());
        String revisionUriInto = getRevisionGraph().getRevisionUri(getBranchNameInto());

        Revision usedSourceRevision = new Revision(getRevisionGraph(), revisionUriFrom, false);
        Revision usedTargetRevision = new Revision(getRevisionGraph(), revisionUriInto, false);

        Branch usedSourceBranch = getRevisionGraph().getBranch(getBranchNameFrom(), true);
        Branch usedTargetBranch = getRevisionGraph().getBranch(getBranchNameInto(), true);

        fullGraphCopy(usedSourceBranch.getReferenceURI(), usedTargetBranch.getReferenceURI());

        return addMetaInformation(usedSourceRevision, usedSourceBranch, usedTargetRevision, usedTargetBranch);
    }

    /**
     * Adds meta information for commit and revision to the revision graph.
     *
     * <img src="{@docRoot}../../doc/revision management description/r43ples-fastforward.png" />
     *
     * @param usedSourceRevision the used source revision (from)
     * @param usedSourceBranch the used source branch (from)
     * @param usedTargetRevision the used target revision (into)
     * @param usedTargetBranch the used target branch (from)
     * @return the created commit
     * @throws InternalErrorException
     */
    private FastForwardMergeCommit addMetaInformation(Revision usedSourceRevision, Branch usedSourceBranch, Revision usedTargetRevision, Branch usedTargetBranch) throws InternalErrorException {

        String commitURI = getRevisionManagement().getNewFastForwardMergeCommitURI(getRevisionGraph(), usedSourceRevision.getRevisionIdentifier(), usedTargetRevision.getRevisionIdentifier());
        String personUri = RevisionManagementOriginal.getUserURI(getUser());

        // Create a new commit (activity)
        StringBuilder queryContent = new StringBuilder(1000);
        queryContent.append(String.format(
                "<%s> a rmo:FastForwardMergeCommit, rmo:MergeCommit, rmo:Commit; "
                        + "	prov:wasAssociatedWith <%s> ;"
                        + "	dc-terms:title \"%s\" ;"
                        + "	prov:atTime \"%s\"^^xsd:dateTime ; %n"
                        + " rmo:usedSourceRevision <%s> ;"
                        + " rmo:usedSourceBranch <%s> ;"
                        + " rmo:usedTargetRevision <%s> ;"
                        + " rmo:usedTargetBranch <%s> .",
                commitURI, personUri, getMessage(), getTimeStamp(),
                usedSourceRevision.getRevisionURI(), usedSourceBranch.getReferenceURI(),
                usedTargetRevision.getRevisionURI(), usedTargetBranch.getReferenceURI()));

        String query = Config.prefixes
                + String.format("INSERT DATA { GRAPH <%s> { %s } }", getRevisionGraph().getRevisionGraphUri(),
                queryContent.toString());

        getTripleStoreInterface().executeUpdateQuery(query);

        updateBelongsTo(usedTargetBranch.getReferenceURI(), getPathCalculationInterface().getPathBetweenStartAndTargetRevision(getRevisionGraph(), usedTargetRevision, usedSourceRevision));
        // Update the full graph of the target branch
        fullGraphCopy(getRevisionGraph().getFullGraphUri(usedSourceBranch.getReferenceURI()), getRevisionGraph().getFullGraphUri(usedTargetBranch.getReferenceURI()));

        // Move branch to new revision
        moveBranchReference(getRevisionGraph().getRevisionGraphUri(), usedTargetBranch.getReferenceURI(), usedTargetRevision.getRevisionURI(), usedSourceRevision.getRevisionURI());
        // Update the target branch object
        usedTargetBranch = getRevisionGraph().getBranch(getBranchNameInto(), true);

        return new FastForwardMergeCommit(getRevisionGraph(), commitURI, getUser(), getTimeStamp(), getMessage(),
                usedSourceRevision, usedSourceBranch, usedTargetRevision, usedTargetBranch, null,
                null, false, null, null);
    }

    /**
     * Adds a new belongs to property to all revision along the specified path.
     *
     * @param branchURI the branch URI
     * @param path the path to update
     * */
    public void updateBelongsTo(String branchURI, Path path){

        Iterator<Revision> revIte = path.getRevisionPath().iterator();
        while(revIte.hasNext()) {
            String revision = revIte.next().getRevisionURI();

            String query = Config.prefixes + String.format("INSERT DATA { GRAPH <%s> { <%s> rmo:belongsTo <%s>. } };%n",
                    getRevisionGraph().getRevisionGraphUri(), revision, branchURI);

            TripleStoreInterfaceSingleton.get().executeUpdateQuery(query);
        }
    }

}




