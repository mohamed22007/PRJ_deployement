package com.dhamet.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'INTÉGRATION : toute l'application Spring démarre (contrôleurs + services +
 * JPA) avec une vraie base H2 en mémoire. On envoie de vraies requêtes HTTP (simulées
 * par MockMvc) et on vérifie les réponses JSON, comme le ferait le frontend.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlayerControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String body(String username, String password) {
        return """
                {"username":"%s","password":"%s","nom":"Test","email":"%s@test.com"}
                """.formatted(username, password, username);
    }

    @Test
    void parcoursComplet_inscription_connexion_profil_deconnexion() throws Exception {
        // 1. Inscription
        mvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("mohamed", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mohamed"));

        // 2. Connexion : on récupère le token
        MvcResult login = mvc.perform(post("/api/players/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mohamed\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonNode node = json.readTree(login.getResponse().getContentAsString());
        String token = node.get("token").asText();

        // 3. Profil avec le token
        mvc.perform(get("/api/players/me").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mohamed"))
                .andExpect(jsonPath("$.credit").value(100))
                .andExpect(jsonPath("$.password").doesNotExist());

        // 4. Déconnexion, puis le token ne marche plus
        mvc.perform(post("/api/players/logout").header("X-Session-Token", token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/players/me").header("X-Session-Token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inscription_doublon_renvoie400() throws Exception {
        mvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("doublon", "secret123")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("doublon", "secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void connexion_mauvaisMotDePasse_renvoie400() throws Exception {
        mvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("sara", "secret123")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/players/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sara\",\"password\":\"faux\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profil_sansSessionValide_renvoie401() throws Exception {
        mvc.perform(get("/api/players/me").header("X-Session-Token", "token-bidon"))
                .andExpect(status().isUnauthorized());
    }
}
