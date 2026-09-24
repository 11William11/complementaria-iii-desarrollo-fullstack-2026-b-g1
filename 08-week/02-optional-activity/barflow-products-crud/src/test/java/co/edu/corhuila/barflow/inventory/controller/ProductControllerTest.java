package co.edu.corhuila.barflow.inventory.controller;

import co.edu.corhuila.barflow.inventory.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de extremo a extremo: HTTP → controller → service → repository → H2. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    private static final String AGUILA = """
            {"name":"Cerveza Aguila","category":"Cervezas","price":5000,"stockQuantity":48}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void fullCrudFlow() throws Exception {
        // Crear → 201 Created con Location
        String body = mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON).content(AGUILA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/products/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        // Listar → 200 con un elemento
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Cerveza Aguila"));

        // Obtener → 200
        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(5000));

        // Actualizar → 200 con los nuevos valores
        mvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cerveza Aguila","category":"Cervezas","price":5500,"stockQuantity":40}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(5500))
                .andExpect(jsonPath("$.stockQuantity").value(40));
        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(jsonPath("$.price").value(5500));

        // Borrar → 204 y luego 404
        mvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void listFiltersByCategory() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(AGUILA));
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Aguardiente Doble Anis","category":"Licores","price":65000,"stockQuantity":12}"""));

        mvc.perform(get("/api/v1/products").param("category", "licores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Aguardiente Doble Anis"));
    }

    @Test
    void createRejectsInvalidBodyWithFieldDetails() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","category":"Cervezas","price":-100,"stockQuantity":10}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void createRejectsDuplicateNameWithConflict() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(AGUILA))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                        .content(AGUILA.replace("Cerveza Aguila", "cerveza aguila")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void unknownOrMalformedIdIsReported() throws Exception {
        mvc.perform(put("/api/v1/products/{id}", "3f1c2d4e-0000-4000-8000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON).content(AGUILA))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/products/{id}", "3f1c2d4e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/products/{id}", "no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }
}
