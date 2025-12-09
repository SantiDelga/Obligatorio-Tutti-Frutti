# TuttiFrutti Singleplayer - Ejecutar

Requisitos:
- JDK 21 instalado y configurado en `JAVA_HOME`.
- Maven instalado.

Ejecutar desde la raíz del repo:

```bat
mvn -f tuttifrutti_singleplayer/pom.xml javafx:run
o click derecho en la carpeta tuttifrutti_singleplayer abrir con terminal y escribir → mvn javafx:run
```

O con exec (si necesitas ver argumentos):

```bat
mvn -f tuttifrutti_singleplayer/pom.xml exec:java -Dexec.mainClass=uy.edu.tuttifrutti.app.MainApp
```

Solución de problemas en IntelliJ:
1. File → Project Structure → Project SDK = JDK 21.
2. Reimportar Maven (Maven Projects → Reimport All).
3. Marcar `src/main/java` como Sources Root si no lo está.
4. File → Invalidate Caches / Restart si sigue mostrando errores.

