package org.acme.kafka.streams.aggregator.query.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.acme.kafka.streams.aggregator.model.WeatherStationTemperature;

@RegisterForReflection
public class WeatherStationData {

    public int stationId;
    public String stationName;
    public double min;
    public double max;
    public int count;
    public double avg;

    WeatherStationData(int stationId, String stationName, double min, double max, int count, double avg) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.min = min;
        this.max = max;
        this.count = count;
        this.avg = avg;
    }

    public WeatherStationData(WeatherStationTemperature ag) {
        this(ag.stationId, ag.stationName, ag.min, ag.max, ag.count, ag.avg);
    }
}
