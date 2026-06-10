package com.krivi.apihistorialmedico.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("API de Carrito de Compras")
            .version("1.0")
            .description("Documentación de la API de Carrito de Compras"))
        .addServersItem(new Server().url("http://192.168.0.128:8080"));
  }
}
