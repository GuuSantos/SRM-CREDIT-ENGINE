CREATE TABLE taxa_base
(
    id            BIGSERIAL PRIMARY KEY,
    taxa_mensal   NUMERIC(19, 8) NOT NULL CHECK (taxa_mensal >= 0),
    vigente_desde TIMESTAMPTZ    NOT NULL,
    criado_por    VARCHAR(50)    NOT NULL,
    criado_em     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_taxa_base_vigencia ON taxa_base (vigente_desde DESC);

CREATE TABLE taxa_cambio
(
    id              BIGSERIAL PRIMARY KEY,
    moeda           VARCHAR(3)     NOT NULL CHECK (moeda = 'USD'),
    brl_por_unidade NUMERIC(19, 8) NOT NULL CHECK (brl_por_unidade > 0),
    vigente_desde   TIMESTAMPTZ    NOT NULL,
    fonte           VARCHAR(10)    NOT NULL CHECK (fonte IN ('PROVEDOR', 'MANUAL')),
    criado_por      VARCHAR(50)    NOT NULL,
    criado_em       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_taxa_cambio_vigencia ON taxa_cambio (moeda, vigente_desde DESC);

INSERT INTO taxa_base (taxa_mensal, vigente_desde, criado_por)
VALUES (0.01, now(), 'sistema');