/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.index.mtas;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.documentservice.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.conll.config.ConllFormatsAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.StatisticsResult;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.index.mtas.config.MtasDocumentIndexAutoConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.inception.search.model", //
        "de.tudarmstadt.ukp.inception.kb.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "repository.path=" + MtasDocumentIndexTest.TEST_OUTPUT_FOLDER })
// REC: Not particularly clear why Propagation.NEVER is required, but if it is not there, the test
// waits forever for the indexing to complete...
@Transactional(propagation = Propagation.NEVER)
@Import({ //
        AnnotationSchemaServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        ConllFormatsAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        ProjectInitializersAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        SearchServiceAutoConfiguration.class, //
        SchedulingServiceAutoConfiguration.class, //
        MtasDocumentIndexAutoConfiguration.class, //
        KnowledgeBaseServiceAutoConfiguration.class })
public class MtasDocumentIndexTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/MtasDocumentIndexTest";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired SearchService searchService;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s=====================\n", methodName,
                aTestInfo.getDisplayName());
    }

    @BeforeEach
    public void setUp()
    {
        if (!userRepository.exists("admin")) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
        }
    }

    private void createProject(Project aProject) throws Exception
    {
        projectService.createProject(aProject);
        projectService.initializeProject(aProject);
    }

    @SafeVarargs
    private final void uploadDocument(Pair<SourceDocument, String>... aDocuments) throws Exception
    {
        Project project = null;
        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            for (Pair<SourceDocument, String> doc : aDocuments) {
                log.info("Uploading document via documentService.uploadSourceDocument: {}", doc);
                project = doc.getLeft().getProject();

                try (InputStream fileStream = new ByteArrayInputStream(
                        doc.getRight().getBytes(UTF_8))) {
                    documentService.uploadSourceDocument(fileStream, doc.getLeft());
                }
            }
        }

        // Avoid the compiler complaining about project not being an effectively final variable
        log.info("Waiting for uploaded documents to be indexed...");
        Project p = project;
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(5, SECONDS) //
                .until(() -> searchService.isIndexValid(p)
                        && searchService.getIndexProgress(p).isEmpty());
        log.info("Indexing complete!");
    }

    private void annotateDocument(Project aProject, User aUser, SourceDocument aSourceDocument)
        throws Exception
    {
        log.info("Preparing annotated document....");

        // Manually build annotated CAS
        JCas jCas = JCasFactory.createJCas();

        JCasBuilder builder = new JCasBuilder(jCas);

        builder.add("The", Token.class);
        builder.add(" ");
        builder.add("capital", Token.class);
        builder.add(" ");
        builder.add("of", Token.class);
        builder.add(" ");

        int begin = builder.getPosition();
        builder.add("Galicia", Token.class);

        NamedEntity ne = new NamedEntity(jCas, begin, builder.getPosition());
        ne.setValue("LOC");
        ne.addToIndexes();

        builder.add(" ");
        builder.add("is", Token.class);
        builder.add(" ");
        builder.add("Santiago", Token.class);
        builder.add(" ");
        builder.add("de", Token.class);
        builder.add(" ");
        builder.add("Compostela", Token.class);
        builder.add(" ");
        builder.add(".", Token.class);

        // Create annotation document
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aSourceDocument, aUser);

        // Write annotated CAS to annotated document
        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            log.info("Writing annotated document using documentService.writeAnnotationCas");
            documentService.writeAnnotationCas(jCas.getCas(), annotationDocument, false);
        }

        log.info("Writing for annotated document to be indexed");
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(5, SECONDS) //
                .until(() -> searchService.isIndexValid(aProject)
                        && searchService.getIndexProgress(aProject).isEmpty());
        log.info("Indexing complete!");
    }

    public void annotateDocumentAdvanced(Project aProject, User aUser,
            SourceDocument aSourceDocument)
        throws Exception
    {
        log.info("Preparing annotated document....");

        // Manually build annotated CAS
        JCas jCas = JCasFactory.createJCas();

        JCasBuilder builder = new JCasBuilder(jCas);

        builder.add("The", Token.class);
        builder.add(" ");
        builder.add("capital", Token.class);
        builder.add(" ");
        builder.add("of", Token.class);
        builder.add(" ");

        int begin = builder.getPosition();
        builder.add("Galicia", Token.class);

        NamedEntity ne = new NamedEntity(jCas, begin, builder.getPosition());
        ne.setValue("LOC");
        ne.addToIndexes();

        builder.add(" ");
        builder.add("is", Token.class);
        builder.add(" ");

        begin = builder.getPosition();

        builder.add("Santiago", Token.class);
        builder.add(" ");
        builder.add("de", Token.class);
        builder.add(" ");
        builder.add("Compostela", Token.class);

        ne = new NamedEntity(jCas, begin, builder.getPosition());
        ne.setValue("LOC");
        ne.addToIndexes();

        builder.add(" ");
        builder.add(".", Token.class);

        Sentence sent = new Sentence(jCas, 0, builder.getPosition());
        sent.addToIndexes();

        // Create annotation document
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aSourceDocument, aUser);

        // Write annotated CAS to annotated document
        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            log.info("Writing annotated document using documentService.writeAnnotationCas");
            documentService.writeAnnotationCas(jCas.getCas(), annotationDocument, false);
        }

        log.info("Writing for annotated document to be indexed");
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(5, SECONDS) //
                .until(() -> searchService.isIndexValid(aProject)
                        && searchService.getIndexProgress(aProject).isEmpty());
        log.info("Indexing complete!");
    }

    @Test
    public void testRawTextQuery() throws Exception
    {
        Project project = new Project();
        project.setName("TestRawTextQuery");

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query = "Galicia";

        // Execute query
        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setLeftContext("The capital of ");
        expectedResult.setText("Galicia");
        expectedResult.setRightContext(" is Santiago de");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void thatLastTokenInDocumentCanBeFound() throws Exception
    {
        Project project = new Project();
        project.setName("LastTokenInDocumentCanBeFound");

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query = "\"\\.\"";

        // Execute query
        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setLeftContext("Santiago de Compostela");
        expectedResult.setText(".");
        expectedResult.setRightContext("");
        expectedResult.setOffsetStart(48);
        expectedResult.setOffsetEnd(49);
        expectedResult.setTokenStart(8);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void testLimitQueryToDocument() throws Exception
    {
        Project project = new Project();
        project.setName("TestLimitQueryToDocument");

        createProject(project);

        SourceDocument sourceDocument1 = new SourceDocument();
        sourceDocument1.setName("Raw text document 1");
        sourceDocument1.setProject(project);
        sourceDocument1.setFormat("text");
        String fileContent1 = "The capital of Galicia is Santiago de Compostela.";

        SourceDocument sourceDocument2 = new SourceDocument();
        sourceDocument2.setName("Raw text document 2");
        sourceDocument2.setProject(project);
        sourceDocument2.setFormat("text");
        String fileContent2 = "The capital of Portugal is Lissabon.";

        uploadDocument(Pair.of(sourceDocument1, fileContent1),
                Pair.of(sourceDocument2, fileContent2));

        User user = userRepository.get("admin");

        String query = "capital";

        // Execute query
        SourceDocument sourceDocument = documentService.getSourceDocument(project,
                "Raw text document 1");
        List<SearchResult> resultsNotLimited = searchService.query(user, project, query);
        List<SearchResult> resultsLimited = searchService.query(user, project, query,
                sourceDocument);

        // Test results
        SearchResult expectedResult1 = new SearchResult();
        expectedResult1.setDocumentId(sourceDocument1.getId());
        expectedResult1.setDocumentTitle("Raw text document 1");
        expectedResult1.setText("capital");
        expectedResult1.setLeftContext("The ");
        expectedResult1.setRightContext(" of Galicia is");
        expectedResult1.setOffsetStart(4);
        expectedResult1.setOffsetEnd(11);
        expectedResult1.setTokenStart(1);
        expectedResult1.setTokenLength(1);

        SearchResult expectedResult2 = new SearchResult();
        expectedResult2.setDocumentId(sourceDocument2.getId());
        expectedResult2.setDocumentTitle("Raw text document 2");
        expectedResult2.setText("capital");
        expectedResult2.setLeftContext("The ");
        expectedResult2.setRightContext(" of Portugal is");
        expectedResult2.setOffsetStart(4);
        expectedResult2.setOffsetEnd(11);
        expectedResult2.setTokenStart(1);
        expectedResult2.setTokenLength(1);

        assertThat(resultsLimited).usingFieldByFieldElementComparator()
                .containsExactly(expectedResult1);

        assertThat(resultsNotLimited).usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResult1, expectedResult2);
    }

    @Test
    public void testSimplifiedTokenTextQuery() throws Exception
    {
        Project project = new Project();
        project.setName("SimplifiedTokenTextQuery");

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query = "\"Galicia\"";

        // Execute query
        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setText("Galicia");
        expectedResult.setLeftContext("The capital of ");
        expectedResult.setRightContext(" is Santiago de");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void testAnnotationQuery() throws Exception
    {
        Project project = new Project();
        project.setName("TestAnnotationQuery");

        createProject(project);

        User user = userRepository.get("admin");

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Annotation document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));
        annotateDocument(project, user, sourceDocument);

        // does not work with capital. the latter two work
        String query = "capital";
        query = "<Named_entity.value=\"LOC\"/>";
        // query = "<Token=\"\"/>";

        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Annotation document");
        // When searching for an annotation, we don't get the matching
        // text back... not sure why...
        expectedResult.setText("");
        expectedResult.setLeftContext("");
        expectedResult.setRightContext("");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void testStatistics() throws Exception
    {
        Project project = new Project();
        project.setName("TestStatistics");

        createProject(project);

        User user = userRepository.get("admin");

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Annotation document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String sourceContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, sourceContent));
        annotateDocumentAdvanced(project, user, sourceDocument);

        SourceDocument otherDocument = new SourceDocument();
        otherDocument.setName("Other document");
        otherDocument.setProject(project);
        otherDocument.setFormat("text");

        String otherContent = "Goodbye moon. Hello World.";

        uploadDocument(Pair.of(otherDocument, otherContent));

        String statistic = "n,min,max,mean,median,standarddeviation";
        OptionalInt minTokenPerDoc = OptionalInt.empty();
        OptionalInt maxTokenPerDoc = OptionalInt.empty();

        String query = "moon";

        StatisticsResult statsResults = searchService.getProjectStatistics(user, project, statistic,
                minTokenPerDoc, maxTokenPerDoc);

        assertThat(statsResults.getMaxTokenPerDoc()).isEqualTo(maxTokenPerDoc);
        assertThat(statsResults.getMinTokenPerDoc()).isEqualTo(minTokenPerDoc);
        assertThat(statsResults.getProject()).isEqualTo(project);
        assertThat(statsResults.getUser()).isEqualTo(user);

        Map<String, Map<String, Double>> expectedResults = new HashMap<String, Map<String, Double>>();

        Map<String, Double> expectedNamedEntity = new HashMap<String, Double>();
        expectedNamedEntity.put("min", 0.0);
        expectedNamedEntity.put("median", 1.0);
        expectedNamedEntity.put("max", 2.0);
        expectedNamedEntity.put("mean", 1.0);
        expectedNamedEntity.put("standarddeviation", Math.pow(2, 0.5));
        expectedNamedEntity.put("Number of Documents", 2.0);
        Map<String, Double> expectedToken = new HashMap<String, Double>();
        expectedToken.put("min", 6.0);
        expectedToken.put("median", 7.5);
        expectedToken.put("max", 9.0);
        expectedToken.put("mean", 7.5);
        expectedToken.put("standarddeviation", Math.pow(4.5, 0.5));
        expectedToken.put("Number of Documents", 2.0);
        Map<String, Double> expectedSentence = new HashMap<String, Double>();
        expectedSentence.put("min", 1.0);
        expectedSentence.put("median", 1.5);
        expectedSentence.put("max", 2.0);
        expectedSentence.put("mean", 1.5);
        expectedSentence.put("standarddeviation", Math.pow(0.5, 0.5));
        expectedSentence.put("Number of Documents", 2.0);
        Map<String, Double> expectedTokenPerSentence = new HashMap<String, Double>();
        expectedTokenPerSentence.put("min", 3.0);
        expectedTokenPerSentence.put("median", 6.0);
        expectedTokenPerSentence.put("max", 9.0);
        expectedTokenPerSentence.put("mean", 6.0);
        expectedTokenPerSentence.put("standarddeviation", Math.pow(18, 0.5));
        expectedTokenPerSentence.put("Number of Documents", 2.0);
        Map<String, Double> expectedNamedEntityPerSentence = new HashMap<String, Double>();
        expectedNamedEntityPerSentence.put("min", 0.0);
        expectedNamedEntityPerSentence.put("median", 1.0);
        expectedNamedEntityPerSentence.put("max", 2.0);
        expectedNamedEntityPerSentence.put("mean", 1.0);
        expectedNamedEntityPerSentence.put("standarddeviation", Math.pow(2, 0.5));
        expectedNamedEntityPerSentence.put("Number of Documents", 2.0);

        expectedResults.put("per Sentence: Token Count", expectedTokenPerSentence);
        expectedResults.put("Named entity.value", expectedNamedEntity);
        expectedResults.put("Token Count", expectedToken);
        expectedResults.put("Sentence Count", expectedSentence);
        expectedResults.put("per Sentence: Named entity.value", expectedNamedEntityPerSentence);

        StatisticsResult queryStatsResults = searchService.getQueryStatistics(user, project,
                statistic, query, minTokenPerDoc, maxTokenPerDoc);

        assertThat(queryStatsResults.getMinTokenPerDoc()).isEqualTo(minTokenPerDoc);
        assertThat(queryStatsResults.getMaxTokenPerDoc()).isEqualTo(maxTokenPerDoc);
        assertThat(queryStatsResults.getUser()).isEqualTo(user);
        assertThat(queryStatsResults.getProject()).isEqualTo(project);

        Map<String, Map<String, Double>> expected = new HashMap<String, Map<String, Double>>();

        Map<String, Double> expectedSearch = new HashMap<String, Double>();
        expectedSearch.put("min", 0.0);
        expectedSearch.put("median", 0.5);
        expectedSearch.put("max", 1.0);
        expectedSearch.put("mean", 0.5);
        expectedSearch.put("standarddeviation", Math.pow(0.5, 0.5));
        expectedSearch.put("Number of Documents", 2.0);
        expectedSearch.put("Number of Hits", 1.0);

        Map<String, Double> expectedSearchPerSentence = new HashMap<String, Double>();
        expectedSearchPerSentence.put("min", 0.0);
        expectedSearchPerSentence.put("median", 0.25);
        expectedSearchPerSentence.put("max", 0.5);
        expectedSearchPerSentence.put("mean", 0.25);
        expectedSearchPerSentence.put("standarddeviation", Math.pow(0.125, 0.5));
        expectedSearchPerSentence.put("Number of Documents", 2.0);
        expectedSearchPerSentence.put("Number of Hits", 1.0);

        expected.put("moon", expectedSearch);
        expected.put("per Sentence: moon", expectedSearchPerSentence);

        assertTrue(expectedResults.equals(statsResults.getNonNullResults()));
        assertTrue(expected.equals(queryStatsResults.getNonNullResults()));

    }

    private static CAS loadCas(String aPathToXmi) throws Exception
    {
        try (FileInputStream fis = new FileInputStream(getResource(aPathToXmi))) {
            JCas jcas = JCasFactory.createJCas();
            CasIOUtils.load(fis, jcas.getCas());
            return jcas.getCas();
        }
    }

    public static File getResource(String aResourceName)
    {
        // return Paths.get("src", "test", "resources", aResourceName).toFile();
        return Paths.get(aResourceName).toFile();
    }

    public void uploadLargeProject(String aFolderPath, Project aProject, User aUser)
        throws Exception
    {

        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            for (final File fileEntry : getResource(aFolderPath).listFiles()) {

                String xmiPath = aFolderPath + "\\" + fileEntry.getName();

                CAS cas = loadCas(xmiPath);

                SourceDocument sourceDocument = new SourceDocument();

                sourceDocument.setName(fileEntry.getName());
                sourceDocument.setProject(aProject);
                sourceDocument.setFormat("text");

                String sourceContent = cas.getDocumentText();

                try (InputStream fileStream = new ByteArrayInputStream(
                        sourceContent.getBytes(UTF_8))) {
                    documentService.uploadSourceDocument(fileStream, sourceDocument);
                }

                documentService.writeAnnotationCas(cas,
                        documentService.createOrGetAnnotationDocument(sourceDocument, aUser),
                        false);
            }
        }
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(5, SECONDS) //
                .until(() -> searchService.isIndexValid(aProject)
                        && searchService.getIndexProgress(aProject).isEmpty());

    }

    @Test
    public void testStatisticsWithRealProject() throws Exception
    {
        long startTime = System.nanoTime();
        Project project = new Project();
        project.setName("TestStatistics");

        createProject(project);

        User user = userRepository.get("admin");
        String folderPath = "D:\\Falko\\Documents\\UKP\\Statistics\\Testdaten\\austen";

        long startUpload = System.nanoTime();
        uploadLargeProject(folderPath, project, user);
        long endUpload = System.nanoTime();
        System.out.println((endUpload - startUpload) / (1000000.0 * 1000.0)); // in seconds

        OptionalInt minTokenPerDoc = OptionalInt.empty();
        OptionalInt maxTokenPerDoc = OptionalInt.empty();
        long afterBuild;
        long afterStats;
        long endTime;
        double durationStats;
        double durationSearch;
        String statistic;
        String query;
        StatisticsResult statsResults;
        StatisticsResult queryStatsResults;

        statistic = "n,min,max,mean,median,standarddeviation";
        query = "the";
        afterBuild = System.nanoTime();
        statsResults = searchService.getProjectStatistics(user, project, statistic, minTokenPerDoc,
                maxTokenPerDoc);
        afterStats = System.nanoTime();
        //queryStatsResults = searchService.getQueryStatistics(user, project, statistic, query,
        //        minTokenPerDoc, maxTokenPerDoc);
        endTime = System.nanoTime();
        durationStats = (afterStats - afterBuild) / (1000000.0 * 1000.0);
        durationSearch = (endTime - afterStats) / (1000000.0 * 1000.0);
        System.out.println(durationStats);
        System.out.println(durationSearch);

        statistic = "max";
        query = "computer";
        afterBuild = System.nanoTime();
        statsResults = searchService.getProjectStatistics(user, project, statistic, minTokenPerDoc,
                maxTokenPerDoc);
        afterStats = System.nanoTime();
        //queryStatsResults = searchService.getQueryStatistics(user, project, statistic, query,
        //        minTokenPerDoc, maxTokenPerDoc);
        endTime = System.nanoTime();
        durationStats = (afterStats - afterBuild) / (1000000.0*100.0);
        durationSearch = (endTime - afterStats) / (1000000.0*100.0);
        System.out.println(durationStats);
        System.out.println(durationSearch);

        assertTrue(0 == 0);
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
    }
}
