package com.veniosg.dir.mvvm.model.search;

import android.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.veniosg.dir.mvvm.model.search.Searcher.SearchRequest.searchRequest;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class SearcherTest {
    private File testFileRoot;
    private Searcher searcher;

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private SearcherLiveData mockResults;
    private File file1;
    private File file2;
    private File file3;
    private File file4;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setUp() throws Exception {
        initMocks(this);

        testFileRoot = new File("testRoot");
        testFileRoot.mkdir();
        File dir1 = new File(testFileRoot, "dir1");
        dir1.mkdir();
        File dir2 = new File(testFileRoot, "dir2");
        dir2.mkdir();
        file1 = new File(dir1, "file");
        file1.createNewFile();
        file2 = new File(dir1, "aSecondOne");
        file2.createNewFile();
        file3 = new File(dir2, "aFile");
        file3.createNewFile();
        file4 = new File(testFileRoot, "thisIsFile3");
        file4.createNewFile();

        searcher = new Searcher(mockResults, trampoline(), trampoline());
    }

    @After
    public void tearDown() throws Exception {
        deleteRecursive(testFileRoot);
    }

    @Test
    public void doesNotFindRoot() throws Exception {
        SearchState expectedSearchState = new SearchState();
        expectedSearchState.setFinished();

        searcher.updateQuery(searchRequest(testFileRoot, testFileRoot.getName()));

        verify(mockResults).setValue(refEq(expectedSearchState));
    }

    @Test
    public void findsAll() throws Exception {
        searcher.updateQuery(searchRequest(testFileRoot, "file"));

        verifyFoundInOrder(mockResults, file4, file1, file3);
    }

    @Test
    public void findsOnlyOne() throws Exception {
        searcher.updateQuery(searchRequest(testFileRoot, "aSecondOne"));

        verifyFoundInOrder(mockResults, file2);
    }

    @Test
    public void findsNothingIfEmptyQuery() {
        SearchState expectedSearchState = new SearchState();
        expectedSearchState.setFinished();
        
        searcher.updateQuery(searchRequest(testFileRoot, ""));

        verify(mockResults).setValue(refEq(expectedSearchState));
    }

    @Test
    public void findsNothingIfNoFilesMatching() throws Exception {
        SearchState expectedSearchState = new SearchState();
        expectedSearchState.setFinished();

        searcher.updateQuery(searchRequest(testFileRoot, "file12455"));

        verify(mockResults).setValue(refEq(expectedSearchState));
    }

    @SuppressWarnings("unchecked")
    private void verifyFoundInOrder(SearcherLiveData mockObservable,
                                    File... expectedFiles) {
        InOrder inOrder = inOrder(mockObservable);

        // Search updates verification
        SearchState searchState = new SearchState();
        for (File file : expectedFiles) {
            searchState.addResult(file.getAbsolutePath());
            inOrder.verify(mockObservable).setValue(refEq(searchState));
        }

        // Search completion verification
        searchState.setFinished();
        inOrder.verify(mockObservable).setValue(refEq(searchState));
    }

    private void deleteRecursive(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                deleteRecursive(c);
        }
        if (f.delete()) {
            f.deleteOnExit();
        }
    }
}