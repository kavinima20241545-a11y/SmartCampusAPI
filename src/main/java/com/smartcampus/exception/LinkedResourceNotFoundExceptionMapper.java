package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 LinkedResourceNotFound Exception Mapper
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        return Response.status(422) // 422 Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "Unprocessable Entity",
                        "status", 422,
                        "message", e.getMessage(),
                        "hint", "Ensure the referenced roomId exists before registering a sensor."
                ))
                .build();
    }
}