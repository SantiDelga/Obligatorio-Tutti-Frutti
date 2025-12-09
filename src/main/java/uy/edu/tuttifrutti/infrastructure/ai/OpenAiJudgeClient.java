package uy.edu.tuttifrutti.infrastructure.ai;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Cliente muy simple que llama a la API de OpenAI usando HTTP directo.
 * Usa la variable de entorno OPENAI_API_KEY.
 */
public class OpenAiJudgeClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";
    private static final Path LOG_FILE = Path.of("openai_judge.log");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;

    public OpenAiJudgeClient() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno OPENAI_API_KEY");
        }
    }

    public String juzgarRonda(GameConfig config,
                              char letra,
                              Map<Categoria, String> respuestasJugador) {
        String prompt = construirPrompt(config, letra, respuestasJugador);
        String bodyJson = construirBodyJson(prompt);

        // Log prompt
        try {
            String logPrompt = "--- PROMPT START ---\n" + prompt + "\n--- PROMPT END ---\n";
            Files.writeString(LOG_FILE, logPrompt, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignore) {
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();
            String content = extraerContenido(json);

            // Log response content
            try {
                String logResp = "--- RESPONSE START ---\n" + content + "\n--- RESPONSE END ---\n";
                Files.writeString(LOG_FILE, logResp, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignore) {
            }

            return content;
        } catch (Exception e) {
            // En caso de error, devolvemos algo legible para debug y lo logueamos
            String err = "ERROR al llamar a OpenAI: " + e.getMessage();
            try {
                Files.writeString(LOG_FILE, "--- ERROR ---\n" + err + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignore) {
            }
            return err;
        }
    }

    private String construirPrompt(GameConfig config,
                                   char letra,
                                   Map<Categoria, String> respuestasJugador) {

        StringBuilder sb = new StringBuilder();

        sb.append("Eres un JUEZ experto y objetivo para el juego Tutti Frutti.\n");
        sb.append("RESPONDE estrictamente en el formato pedido y nada más.\n\n");

        sb.append("Reglas de validación (ORDEN de prioridad):\n");
        sb.append("1) COMPROBACIÓN FACTUAL: la respuesta debe EXISTIR como palabra/nombre real en el uso común o en fuentes geográficas/biológicas fiables.\n");
        sb.append("2) PERTINENCIA DE CATEGORÍA: la respuesta debe pertenecer a la categoría indicada (por ejemplo, 'Ciudad'→ciudad real; 'Animal'→especie real; 'Objeto'→objeto tangible/común).\n");
        sb.append("3) LETRA: la palabra debe comenzar por la letra indicada (case-insensitive).\n\n");

        sb.append("REGLA CLAVE: Devuelve SI únicamente si las tres condiciones anteriores (existencia, pertenencia a la categoría y comienza con la letra) se cumplen simultáneamente.\n");
        sb.append("Si alguna de las condiciones NO se cumple, devuelve NO.\n");
        sb.append("Si la respuesta está vacía o solo espacios, devuelve VACIA.\n\n");

        sb.append("Instrucciones de comprobación por categorías (ejemplos y cómo evaluarlas):\n");
        sb.append("- Categoria 'Ciudad': acepta nombres de ciudades reales reconocidas (ej.: 'Valparaíso', 'Jakarta', 'Yucatán' si corresponde a un topónimo). Rechaza cadenas sin significado geográfico como 'jaja'.\n");
        sb.append("- Categoria 'Animal': acepta nombres de especies o animales comunes (ej.: 'koala', 'venado'). Rechaza palabras inventadas o nonsense.\n");
        sb.append("- Categoria 'Objeto': acepta objetos concretos y comunes (ej.: 'válvula', 'jarrón'). Rechaza cadenas aleatorias/nonsense.\n");
        sb.append("- Para cualquier otra categoría, aplica el mismo criterio: existencia fáctica + pertenencia semántica.\n\n");

        sb.append("Formato de salida OBLIGATORIO (línea por categoría, sin texto extra):\n");
        sb.append("- Categoria: <nombre> | Respuesta: <texto> | Estado: SI/NO/VACIA | Motivo si el estado es NO.\n\n");

        sb.append("Ejemplos (debe producir exactamente estos Estados):\n");
        sb.append("- letra=K, categoria=Animal, respuesta=koala  => SI  (koala existe y es un animal y empieza con K)\n");
        sb.append("- categoria=Ciudad, respuesta=jaja           => NO  (no es una ciudad real)\n");
        sb.append("- categoria=Objeto, respuesta=asdfgh         => NO  (nonsense)\n");
        sb.append("- respuesta vacía                              => VACIA\n\n");

        sb.append("Respuestas del jugador (comprueba cada una según las reglas y produce una línea por cada):\n");
        for (Map.Entry<Categoria, String> entry : respuestasJugador.entrySet()) {
            sb.append("- Categoria: ").append(entry.getKey().getNombre())
                    .append(" | Respuesta: ").append(entry.getValue()).append("\n");
        }

        sb.append("\nIMPORTANTE: devuelve al final una línea exacta: Puntaje total: X  (donde X es el número de SI).\n");
        sb.append("NO añadas ningún otro texto, explicación o numeración.\n");

        return sb.toString();
    }

    /** Arma el JSON de la request para /v1/chat/completions */
    private String construirBodyJson(String prompt) {
        String promptEscapado = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        return """
               {
                 "model": "%s",
                 "messages": [
                   { "role": "system", "content": "Eres un juez de Tutti Frutti." },
                   { "role": "user", "content": "%s" }
                 ],
                 "temperature": 0.0
               }
               """.formatted(MODEL, promptEscapado);
    }

    /**
     * Extrae el campo "content" del primer mensaje devuelto.
     * Es un parser muy simple, suficiente para el obligatorio.
     */
    private String extraerContenido(String json) {
        int idx = json.indexOf("\"content\":");
        if (idx == -1) {
            return json; // por si cambia el formato, al menos vemos el JSON
        }
        int firstQuote = json.indexOf('"', idx + 10);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (firstQuote == -1 || secondQuote == -1) {
            return json;
        }

        String raw = json.substring(firstQuote + 1, secondQuote);
        return raw
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
