/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.schwehla.matrosdms.domain.api.ApiErrorResponse;
import net.schwehla.matrosdms.domain.api.BroadcastMessage;
import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.service.message.ProgressMessage;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "BearerAuth";

	@Bean
	public OpenAPI matrosOpenAPI() {
		return new OpenAPI()
				.components(
						new Components()
								// Register Core Schemas explicitly to ensure clean TS generation
								.addSchemas("BroadcastMessage", resolveSchema(BroadcastMessage.class))
								.addSchemas("PipelineStatusMessage", resolveSchema(PipelineStatusMessage.class))
								.addSchemas("InboxFile", resolveSchema(InboxFile.class))
								.addSchemas("ProgressMessage", resolveSchema(ProgressMessage.class))
								.addSecuritySchemes(
										SECURITY_SCHEME_NAME,
										new SecurityScheme()
												.name(SECURITY_SCHEME_NAME)
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.info(
						new Info()
								.title("MatrosDMS API")
								.version("1.0")
								.description(
										"# Document Management System API\n\n"
												+ "Server-Sent Events (SSE) are available at `/api/stream/updates`.")
								.contact(new Contact().name("MatrosDMS Team")));
	}

	// Helper to resolve schema + references
	private Schema resolveSchema(Class<?> clazz) {
		return ModelConverters.getInstance().readAllAsResolvedSchema(clazz).schema;
	}

	@Bean
	public OpenApiCustomizer globalErrorResponses() {
		return openApi -> {
			ensureSchemaExists(openApi, ApiErrorResponse.class, "ApiErrorResponse");

			openApi
					.getPaths()
					.values()
					.forEach(
							pathItem -> pathItem
									.readOperations()
									.forEach(
											operation -> {
												ApiResponses responses = operation.getResponses();
												responses.addApiResponse("400", createErrorResponse("Bad Request"));
												responses.addApiResponse("401", createErrorResponse("Unauthorized"));
												responses.addApiResponse("403", createErrorResponse("Forbidden"));
												responses.addApiResponse("404", createErrorResponse("Not Found"));
												responses.addApiResponse("500", createErrorResponse("Server Error"));
											}));
		};
	}

	private void ensureSchemaExists(OpenAPI openApi, Class<?> clazz, String name) {
		if (!openApi.getComponents().getSchemas().containsKey(name)) {
			ResolvedSchema resolved = ModelConverters.getInstance().readAllAsResolvedSchema(clazz);
			openApi.getComponents().addSchemas(name, resolved.schema);
			if (resolved.referencedSchemas != null) {
				openApi.getComponents().getSchemas().putAll(resolved.referencedSchemas);
			}
		}
	}

	private ApiResponse createErrorResponse(String description) {
		return new ApiResponse()
				.description(description)
				.content(
						new Content()
								.addMediaType(
										org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
										new MediaType()
												.schema(
														new Schema<ApiErrorResponse>()
																.$ref("#/components/schemas/ApiErrorResponse"))));
	}

	@Bean
	public OpenApiCustomizer nullableDatesCustomizer() {
		return openApi -> {
			if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null)
				return;
			openApi
					.getComponents()
					.getSchemas()
					.values()
					.forEach(
							schema -> {
								Map<String, Schema> properties = schema.getProperties();
								if (properties != null) {
									properties.forEach(
											(key, prop) -> {
												if ("date-time".equals(prop.getFormat())
														|| "date".equals(prop.getFormat())) {
													prop.setNullable(true);
												}
											});
								}
							});
		};
	}
}
