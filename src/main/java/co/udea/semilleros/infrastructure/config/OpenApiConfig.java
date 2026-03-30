package co.udea.semilleros.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:dev}")
    private String perfil;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema de Semilleros de Investigación UdeA")
                        .description("""
                                API REST para la gestión de semilleros de investigación de la
                                Universidad de Antioquia.
                                
                                **Perfiles disponibles:** dev, cert, pdn
                                
                                **Autenticación:** Los endpoints del coordinador requieren token JWT.
                                Obtén el token en `POST /api/v1/auth/login` y envíalo en el header:
                                `Authorization: Bearer {token}`
                                
                                **Restricción de dominio:** Solo se permiten correos `@udea.edu.co`
                                para inscripciones y autenticación de coordinadores.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Vicerrectoría de Investigación - UdeA")
                                .email("investigacion@udea.edu.co"))
                        .license(new License().name("Uso interno UdeA")))
                .servers(List.of(
                        new Server().url("/").description("Servidor actual (" + perfil + ")")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT obtenido en /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
