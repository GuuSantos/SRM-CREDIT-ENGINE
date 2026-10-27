package br.com.srm.creditengine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class RelogioControlado extends Clock {

    private final Instant inicio;
    private volatile Instant agora;

    public RelogioControlado(Instant inicio) {
        this.inicio = inicio;
        this.agora = inicio;
    }

    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    public void reiniciar() {
        agora = inicio;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.fixed(agora, zona);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}