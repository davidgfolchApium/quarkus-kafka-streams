package org.acme.kafka.streams.aggregator.streams;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.acme.kafka.streams.aggregator.model.Aggregation;
import org.acme.kafka.streams.aggregator.model.Temperature;
import org.acme.kafka.streams.aggregator.model.WeatherStation;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.Instant.now;
import static org.acme.kafka.streams.aggregator.model.Temperature.SEP;
import static org.acme.kafka.streams.aggregator.streams.TestHelper.*;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration testing of the application with an embedded broker.
 */
@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
public class AggregatorTest {

    private static final Map<String, String> CONSUMER_CFG = new HashMap<>(Map.of(
            BOOTSTRAP_SERVERS_CONFIG, "KafkaResource.getBootstrapServers() DONT WORK STATICALLY",
            ConsumerConfig.GROUP_ID_CONFIG, "test-group-id",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"));
    private static final WeatherStation WEATHER_STATION = new WeatherStation(1, "Station 1");


    KafkaProducer<Integer, String> temperatureP;
    KafkaProducer<Integer, WeatherStation> stationP;
    KafkaConsumer<Integer, Aggregation> aggC;

    @BeforeEach
    public void setUp() {
        final var prodCfg = Map.of(BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        temperatureP = newProducerIntString(prodCfg);
        stationP = newProducerIntOm(prodCfg);
        CONSUMER_CFG.put(BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        aggC = newConsumerIntOm(CONSUMER_CFG, Aggregation.class);
    }

    @AfterEach
    public void tearDown() {
        temperatureP.close();
        stationP.close();
        aggC.close();
    }

    @Test
    @Timeout(value = 30)
    public void test() {
        aggC.subscribe(List.of(Aggregation.TOPIC));
        stationP.send(new ProducerRecord<>(WeatherStation.TOPIC, 1, WEATHER_STATION));
        sendTemperature("15");
        sendTemperature("25");
        Aggregation r = poll(aggC, 1).get(0).value();
        assertEquals(0, r.count % 2); //should be 2, but repeated local test run accumulates +2
        assertEquals(WEATHER_STATION.id, r.stationId);
        assertEquals(WEATHER_STATION.name, r.stationName);
        assertEquals(20, r.avg);
    }

    private void sendTemperature(String temperature) {
        temperatureP.send(new ProducerRecord<>(Temperature.TOPIC, 1, now() + Temperature.SEP + temperature));
    }

}
