package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.DataStore;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 SensorReadingResource class
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }


    @GET
    public Response getReadings() {
        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }


    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is under MAINTENANCE and cannot accept new readings."
            );
        }
        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is OFFLINE and cannot accept new readings."
            );
        }

        // Auto-generate ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading = new SensorReading(reading.getValue());
        } else {
            if (reading.getTimestamp() == 0) {
                reading.setTimestamp(System.currentTimeMillis());
            }
        }

        store.addReading(sensorId, reading);

        // update parent sensor's currentValue for data consistency
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(Map.of(
                "message", "Reading recorded successfully.",
                "sensorId", sensorId,
                "reading", reading,
                "updatedSensorValue", sensor.getCurrentValue()
        )).build();
    }


    @GET
    @Path("/{readingId}")
    public Response getReading(@PathParam("readingId") String readingId) {
        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return history.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Reading '" + readingId + "' not found for sensor '" + sensorId + "'."))
                        .build());
    }
}