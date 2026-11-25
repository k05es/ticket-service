package com.k05es.ticketservice.config;

import com.k05es.ticketservice.dto.response.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Настройка Swagger/OpenAPI с поддержкой JWT Bearer и типовых ответов об ошибках ApiError. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ticket Service API",
                version = "1.0",
                description = "REST API сервиса обработки заявок (ЛР №4: Spring Security, роли и логирование). "
                        + "Получите токен в POST /api/auth/login и укажите его через Authorize."),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT-токен из POST /api/auth/login (вводится без слова Bearer). Администратор: admin / admin123.")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenApiCustomizer commonErrorResponsesCustomizer() {
        return openApi -> {
            registerApiErrorSchema(openApi);
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> {
                boolean publicPath = path.startsWith("/api/auth");
                for (Operation operation : item.readOperations()) {
                    ApiResponses responses = operation.getResponses();
                    if (responses == null) {
                        responses = new ApiResponses();
                        operation.setResponses(responses);
                    }
                    addIfAbsent(responses, "400", "Некорректный запрос или ошибка валидации");
                    if (!publicPath) {
                        addIfAbsent(responses, "401", "Не выполнена аутентификация");
                        addIfAbsent(responses, "403", "Недостаточно прав для роли пользователя");
                    }
                    addIfAbsent(responses, "500", "Внутренняя ошибка сервера");
                    attachErrorBody(responses);
                }
            });
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerApiErrorSchema(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        Map<String, Schema> schemas = ModelConverters.getInstance().readAll(ApiError.class);
        schemas.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
    }

    private void addIfAbsent(ApiResponses responses, String code, String description) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().description(description));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void attachErrorBody(ApiResponses responses) {
        responses.forEach((code, response) -> {
            boolean isError = code.startsWith("4") || code.startsWith("5");
            if (errorWithoutContent(isError, response)) {
                Schema ref = new Schema().$ref("#/components/schemas/ApiError");
                response.setContent(new Content()
                        .addMediaType("application/json", new MediaType().schema(ref)));
            }
        });
    }

    private boolean errorWithoutContent(boolean isError, ApiResponse response) {
        return isError && (response.getContent() == null || response.getContent().isEmpty());
    }
}
