-- Geographical scoping: state master + agreement-to-state mapping

CREATE TABLE state_master (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    state_name          VARCHAR(255)    NOT NULL,
    state_code          VARCHAR(10)     NOT NULL,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_by_user_id  BIGINT          NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by_user_id  BIGINT          NULL,
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_state_code UNIQUE (state_code),
    INDEX idx_state_name (state_name),
    INDEX idx_state_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agreement_states (
    agreement_id    BIGINT  NOT NULL,
    state_id        BIGINT  NOT NULL,
    PRIMARY KEY (agreement_id, state_id),
    CONSTRAINT fk_agreement_states_agreement FOREIGN KEY (agreement_id) REFERENCES agreements (id),
    CONSTRAINT fk_agreement_states_state FOREIGN KEY (state_id) REFERENCES state_master (id),
    INDEX idx_agreement_states_state_id (state_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO state_master (state_name, state_code, is_active) VALUES
    ('Maharashtra', 'MH', 1),
    ('Telangana', 'TS', 1),
    ('Delhi', 'DL', 1),
    ('Tamil Nadu', 'TN', 1),
    ('Karnataka', 'KA', 1),
    ('Gujarat', 'GJ', 1),
    ('West Bengal', 'WB', 1),
    ('Uttar Pradesh', 'UP', 1);
