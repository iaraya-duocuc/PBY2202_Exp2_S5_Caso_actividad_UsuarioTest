# Backend PetSalud

Con Java 17, ejecuta desde la raíz del proyecto:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

La API queda en http://localhost:8080. H2 guarda los datos en memoria y los descarta al apagar la aplicación.

## Pruebas

```bash
./mvnw clean verify -Dspring.profiles.active=h2
```

Maven ejecuta las pruebas, verifica el mínimo de cobertura y genera el reporte en `target/site/jacoco/index.html`.
