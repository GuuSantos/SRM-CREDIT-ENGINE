CREATE TABLE cedente (
                         id           BIGSERIAL    PRIMARY KEY,
                         cnpj         VARCHAR(14)  NOT NULL UNIQUE CHECK (cnpj ~ '^[0-9]{14}$'),
    razao_social VARCHAR(150) NOT NULL
);

CREATE TABLE recebivel (
                           id         BIGSERIAL      PRIMARY KEY,
                           cedente_id BIGINT         NOT NULL REFERENCES cedente (id),
                           tipo       VARCHAR(30)    NOT NULL CHECK (tipo IN ('DUPLICATA_MERCANTIL', 'CHEQUE_PRE_DATADO')),
                           numero     VARCHAR(30)    NOT NULL,
                           valor_face NUMERIC(19, 2) NOT NULL CHECK (valor_face > 0),
                           vencimento DATE           NOT NULL,
                           status     VARCHAR(10)    NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'LIQUIDADO')),
                           version    BIGINT         NOT NULL DEFAULT 0,
                           criado_por VARCHAR(50)    NOT NULL,
                           criado_em  TIMESTAMPTZ    NOT NULL,
                           CONSTRAINT uk_recebivel_titulo UNIQUE (cedente_id, tipo, numero)
);

CREATE TABLE cotacao (
                         id                 UUID           PRIMARY KEY,
                         recebivel_id       BIGINT         NOT NULL REFERENCES recebivel (id),
                         taxa_base_id       BIGINT         NOT NULL REFERENCES taxa_base (id),
                         taxa_cambio_id     BIGINT         REFERENCES taxa_cambio (id),
                         taxa_base_mensal   NUMERIC(19, 8) NOT NULL,
                         spread_mensal      NUMERIC(19, 8) NOT NULL,
                         prazo_dias         INTEGER        NOT NULL CHECK (prazo_dias > 0),
                         moeda_pagamento    VARCHAR(3)     NOT NULL CHECK (moeda_pagamento IN ('BRL', 'USD')),
                         cambio_brl_por_usd NUMERIC(19, 8),
                         valor_presente_brl NUMERIC(19, 2) NOT NULL,
                         desagio            NUMERIC(19, 2) NOT NULL,
                         valor_pagamento    NUMERIC(19, 2) NOT NULL,
                         expira_em          TIMESTAMPTZ    NOT NULL,
                         criado_por         VARCHAR(50)    NOT NULL,
                         criado_em          TIMESTAMPTZ    NOT NULL,
                         CONSTRAINT ck_cotacao_cambio CHECK (
                             (moeda_pagamento = 'BRL' AND taxa_cambio_id IS NULL AND cambio_brl_por_usd IS NULL) OR
                             (moeda_pagamento = 'USD' AND taxa_cambio_id IS NOT NULL AND cambio_brl_por_usd IS NOT NULL)
                             )
);

CREATE INDEX idx_cotacao_recebivel ON cotacao (recebivel_id);

CREATE TABLE liquidacao (
                            id                 BIGSERIAL      PRIMARY KEY,
                            cotacao_id         UUID           NOT NULL REFERENCES cotacao (id),
                            recebivel_id       BIGINT         NOT NULL REFERENCES recebivel (id),
                            cedente_id         BIGINT         NOT NULL REFERENCES cedente (id),
                            tipo               VARCHAR(30)    NOT NULL,
                            valor_face         NUMERIC(19, 2) NOT NULL,
                            valor_presente_brl NUMERIC(19, 2) NOT NULL,
                            desagio            NUMERIC(19, 2) NOT NULL,
                            moeda_pagamento    VARCHAR(3)     NOT NULL,
                            valor_pagamento    NUMERIC(19, 2) NOT NULL,
                            cambio_brl_por_usd NUMERIC(19, 8),
                            taxa_base_mensal   NUMERIC(19, 8) NOT NULL,
                            spread_mensal      NUMERIC(19, 8) NOT NULL,
                            prazo_dias         INTEGER        NOT NULL,
                            liquidado_por      VARCHAR(50)    NOT NULL,
                            liquidado_em       TIMESTAMPTZ    NOT NULL,
                            CONSTRAINT uk_liquidacao_cotacao UNIQUE (cotacao_id)
);

CREATE INDEX idx_liquidacao_periodo ON liquidacao (liquidado_em DESC);
CREATE INDEX idx_liquidacao_cedente_moeda ON liquidacao (cedente_id, moeda_pagamento, liquidado_em DESC);

CREATE FUNCTION bloquear_alteracao_liquidacao() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Liquidações são imutáveis: operação % não permitida', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_liquidacao_imutavel
    BEFORE UPDATE OR DELETE ON liquidacao
    FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao_liquidacao();

INSERT INTO cedente (cnpj, razao_social) VALUES
                                             ('12345678000195', 'Metalúrgica Alfa Ltda'),
                                             ('98765432000198', 'Comercial Beta S.A.'),
                                             ('45678912000155', 'Têxtil Gama Ltda');