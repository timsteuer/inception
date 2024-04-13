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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.dkpro.statistics.agreement.coding.ICodingAnnotationItem;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha.KrippendorffAlphaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

public class KrippendorffAlphaNominalAgreementMeasureTest
    extends AgreementMeasureTestSuite_ImplBase
{
    private AgreementMeasureSupport<DefaultAgreementTraits, //
            FullCodingAgreementResult, ICodingAnnotationStudy> sut;
    private DefaultAgreementTraits traits;

    @Override
    @BeforeEach
    public void setup()
    {
        super.setup();

        sut = new KrippendorffAlphaAgreementMeasureSupport(annotationService);
        traits = sut.createTraits();
    }

    @Test
    public void multiLinkWithRoleLabelDifference() throws Exception
    {
        when(annotationService.listSupportedFeatures(any(Project.class))).thenReturn(features);

        var result = multiLinkWithRoleLabelDifferenceTest(sut);

        DiffResult diff = result.getDiff();

        diff.print(System.out);

        assertEquals(3, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(2, diff.getIncompleteConfigurationSets().size());

        assertEquals(NaN, result.getAgreement(), 0.00001d);
    }

    @Test
    public void twoEmptyCasTest() throws Exception
    {
        var result = twoEmptyCasTest(sut);

        DiffResult diff = result.getDiff();

        assertEquals(0, diff.size());
        assertEquals(0, diff.getDifferingConfigurationSets().size());
        assertEquals(0, diff.getIncompleteConfigurationSets().size());

        assertEquals(NaN, result.getAgreement(), 0.000001d);
        assertEquals(0, result.getIncompleteSetsByPosition().size());
    }

    // @Test
    // public void singleNoDifferencesWithAdditionalCasTest() throws Exception
    // {
    // var result = singleNoDifferencesWithAdditionalCasTest(sut);
    //
    // CodingAgreementResult result1 = agreement.getStudy("user1", "user2");
    // assertEquals(0, result1.getTotalSetCount());
    // assertEquals(0, result1.getIrrelevantSets().size());
    // assertEquals(0, result1.getRelevantSetCount());
    //
    // CodingAgreementResult result2 = agreement.getStudy("user1", "user3");
    // assertEquals(1, result2.getTotalSetCount());
    // assertEquals(0, result2.getIrrelevantSets().size());
    // assertEquals(1, result2.getRelevantSetCount());
    //
    // assertEquals(NaN, agreement.getStudy("user1", "user2").getAgreement(), 0.01);
    // assertEquals(NaN, agreement.getStudy("user1", "user3").getAgreement(), 0.01);
    // assertEquals(NaN, agreement.getStudy("user2", "user3").getAgreement(), 0.01);
    // }

    @Test
    public void testTwoWithoutLabel_noExcludeIncomplete() throws Exception
    {
        traits.setExcludeIncomplete(false);

        var result = twoWithoutLabelTest(sut, traits);

        ICodingAnnotationItem item1 = result.getStudy().getItem(0);
        ICodingAnnotationItem item2 = result.getStudy().getItem(1);
        ICodingAnnotationItem item3 = result.getStudy().getItem(2);
        assertEquals("", item1.getUnit(0).getCategory());
        assertEquals("", item1.getUnit(1).getCategory());
        assertEquals("", item2.getUnit(0).getCategory());
        assertEquals(null, item2.getUnit(1).getCategory());
        assertEquals(null, item3.getUnit(0).getCategory());
        assertEquals("", item3.getUnit(1).getCategory());

        assertEquals(4, result.getTotalSetCount());
        assertEquals(0, result.getIrrelevantSets().size());
        // the following two counts are zero because the incomplete sets are not excluded!
        assertEquals(2, result.getIncompleteSetsByPosition().size());
        assertEquals(0, result.getIncompleteSetsByLabel().size());
        assertEquals(3, result.getSetsWithDifferences().size());
        assertEquals(4, result.getRelevantSetCount());
        assertEquals(0.4, result.getAgreement(), 0.01);
    }

    @Test
    public void fullSingleCategoryAgreementWithTagsetTest() throws Exception
    {
        TagSet tagset = new TagSet(project, "tagset");
        Tag tag1 = new Tag(tagset, "+");
        Tag tag2 = new Tag(tagset, "-");
        when(annotationService.listTags(tagset)).thenReturn(asList(tag1, tag2));
        when(annotationService.listSupportedFeatures(any(Project.class))).thenReturn(features);

        var result = fullSingleCategoryAgreementWithTagset(sut, traits);

        ICodingAnnotationItem item1 = result.getStudy().getItem(0);
        assertEquals("+", item1.getUnit(0).getCategory());

        assertEquals(1, result.getTotalSetCount());
        assertEquals(0, result.getIrrelevantSets().size());
        assertEquals(0, result.getIncompleteSetsByPosition().size());
        assertEquals(0, result.getIncompleteSetsByLabel().size());
        assertEquals(0, result.getSetsWithDifferences().size());
        assertEquals(1, result.getRelevantSetCount());
        assertEquals(1.0, result.getAgreement(), 0.01);
    }
}
