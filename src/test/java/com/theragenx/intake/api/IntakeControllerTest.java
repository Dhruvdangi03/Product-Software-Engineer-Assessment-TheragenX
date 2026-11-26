package com.theragenx.intake.api;

import com.theragenx.intake.catalog.Catalog;
import com.theragenx.intake.catalog.FieldDefinition;
import com.theragenx.intake.catalog.FieldType;
import com.theragenx.intake.support.TempDirs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The API against the committed outputs. Reviews go to a temporary folder. */
@SpringBootTest
@AutoConfigureMockMvc
class IntakeControllerTest {

    private static final String DOCUMENT_WITH_A_CASE = "D01";

    @DynamicPropertySource
    static void reviewsInATemporaryFolder(DynamicPropertyRegistry registry) {
        registry.add("intake.reviews-dir", () -> TempDirs.create("reviews").toString());
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Catalog catalog;

    @Test
    void servesTheCatalog() throws Exception {
        mvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalog_version").value(catalog.catalogVersion()))
                .andExpect(jsonPath("$.fields", hasSize(catalog.fields().size())));
    }

    @Test
    void listsDocumentsWithTitleCaseCountAndFlags() throws Exception {
        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DOCUMENT_WITH_A_CASE))
                .andExpect(jsonPath("$[0].title").value("Suspected adverse drug reaction"))
                .andExpect(jsonPath("$[0].case_count").value(1))
                .andExpect(jsonPath("$[0].flags").isArray());
    }

    @Test
    void servesTextAndExtractionOfOneDocument() throws Exception {
        mvc.perform(get("/api/documents/" + DOCUMENT_WITH_A_CASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", notNullValue()))
                .andExpect(jsonPath("$.extraction.cases", hasSize(1)))
                .andExpect(jsonPath("$.review_progress.total").value(catalog.fields().size()));
    }

    @Test
    void answersNotFoundForAnUnknownDocument() throws Exception {
        mvc.perform(get("/api/documents/NOPE")).andExpect(status().isNotFound());
    }

    @Test
    void recordsAReviewDecision() throws Exception {
        String fieldId = catalog.fields().getFirst().id();

        mvc.perform(patch(fieldUrl(0, fieldId)).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"accept\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cases['0']['" + fieldId + "'].decision").value("accepted"));
    }

    @Test
    void rejectsAnEditThatFailsTheCatalog() throws Exception {
        FieldDefinition coded = catalog.fields().stream().filter(f -> f.type() == FieldType.CODED).findFirst().orElseThrow();

        mvc.perform(patch(fieldUrl(0, coded.id())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"edit\", \"value\": \"not-an-option\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("not one of the options")));
    }

    @Test
    void answersNotFoundForAFieldOrCaseThatDoesNotExist() throws Exception {
        mvc.perform(patch(fieldUrl(0, "no_such_field")).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"accept\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch(fieldUrl(9, catalog.fields().getFirst().id())).contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"accept\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesDuplicatePairsWithAFieldComparison() throws Exception {
        mvc.perform(get("/api/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private static String fieldUrl(int caseIndex, String fieldId) {
        return "/api/documents/" + DOCUMENT_WITH_A_CASE + "/cases/" + caseIndex + "/fields/" + fieldId;
    }
}
