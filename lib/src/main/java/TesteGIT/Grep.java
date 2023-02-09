package TesteGIT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

public class Grep {

    private final Repository repository;
    private final Pattern pattern;

    private final String revName;

    public Grep(Repository repository, Pattern pattern, String revName) {
        this.repository = repository;
        this.pattern = pattern;
        this.revName = revName;
    }

    public void grepPrintingResults() throws IOException {
        ObjectReader objectReader = repository.newObjectReader();
        try {
            ObjectId commitId = repository.resolve(revName);
            impl(objectReader, commitId);
        } finally {
            objectReader.close();
        }
    }

    private void impl(ObjectReader objectReader, ObjectId commitId)
            throws IOException {

        TreeWalk treeWalk = new TreeWalk(objectReader);
        RevWalk revWalk = new RevWalk(objectReader);
        RevCommit commit = revWalk.parseCommit(commitId);

        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        treeParser.reset(objectReader, commit.getTree());

        int treeIndex = treeWalk.addTree(treeParser);
        treeWalk.setRecursive(true);

        while (treeWalk.next()) {
            AbstractTreeIterator it = treeWalk.getTree(treeIndex,
                    AbstractTreeIterator.class);
            ObjectId objectId = it.getEntryObjectId();
            ObjectLoader objectLoader = objectReader.open(objectId);
            System.out.println(objectId.getName());
            if (!isBinary(objectLoader.openStream())) {
                List<String> matchedLines = getMatchedLines(objectLoader
                        .openStream());
                if (!matchedLines.isEmpty()) {
                    String path = it.getEntryPathString();
                    System.out.println(path);
//                    for (String matchedLine : matchedLines) {
//                        System.out.println(path);
//                    }
                }
            }
        }
    }

    private List<String> getMatchedLines(InputStream stream) throws IOException {
        BufferedReader buf = null;
        try {
            List<String> matchedLines = new ArrayList<String>();
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
            buf = new BufferedReader(reader);
            String line;
            while ((line = buf.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    matchedLines.add(line);
                }
            }
            return matchedLines;
        } finally {
            if (buf != null) {
                buf.close();
            }
        }
    }

    private static boolean isBinary(InputStream stream) throws IOException {
        try {
            return RawText.isBinary(stream);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore, we were just reading
            }
        }
    }
}