package uy.edu.tuttifrutti.infrastructure.ai;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Cliente muy simple que llama a la API de OpenAI usando HTTP directo.
 * Usa la variable de entorno OPENAI_API_KEY.
 */
public class OpenAiJudgeClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";

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
            return extraerContenido(json);
        } catch (Exception e) {
            // En caso de error, devolvemos algo legible para debug
            return "ERROR al llamar a OpenAI: " + e.getMessage();
        }
    }

    private String construirPrompt(GameConfig config,
                                   char letra,
                                   Map<Categoria, String> respuestasJugador) {

        StringBuilder sb = new StringBuilder();
        sb.append("Actúa como juez de un juego de Tutti Frutti (Stop).\n")
                .append("La letra de esta ronda es: ").append(letra).append(".\n")
                .append("Para cada categoría, te paso la respuesta del jugador.\n")
                .append("Debes verificar tres cosas:\n")
                .append("1) Que la palabra empiece con esa letra.\n")
                .append("2) Que exista en español (o sea razonable como palabra/nombre propio).\n")
                .append("3) Que pertenezca a la categoría indicada.\n\n")
                .append("Devuélveme un resumen en formato claro, por ejemplo:\n")
                .append("- Categoria: <nombre> | Respuesta: <texto> | Estado: VALIDA/INVALIDA/VACIA | Motivo si es inválida.\n")
                .append("Al final, dame un 'Puntaje total: X' donde X es la cantidad de válidas.\n\n")
                .append("Respuestas del jugador:\n");

        for (Map.Entry<Categoria, String> entry : respuestasJugador.entrySet()) {
            sb.append("- Categoria: ").append(entry.getKey().getNombre())
                    .append(" | Respuesta: ").append(entry.getValue()).append("\n");
        }

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
                 "temperature": 0.1
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
