CREATE TABLE protocol_run (
    id             UUID         PRIMARY KEY,
    task_id        UUID         NOT NULL REFERENCES task (id),
    protocol_type  VARCHAR(50)  NOT NULL,
    current_state  VARCHAR(30)  NOT NULL,
    attempt        INT          NOT NULL,
    failure_detail TEXT,
    started_at     TIMESTAMPTZ  NOT NULL,
    completed_at   TIMESTAMPTZ,
    version        BIGINT       NOT NULL,

    CONSTRAINT ck_protocol_run_protocol_type CHECK (protocol_type IN ('PROBLEM_SOLVER')),
    CONSTRAINT ck_protocol_run_state CHECK (current_state IN (
        'ANALYZE', 'PLAN', 'DISPATCH', 'SOLVE', 'EXECUTE', 'EVALUATE',
        'REPAIR', 'OPTIMIZE', 'REPORT', 'DONE', 'FAILED')),
    CONSTRAINT ck_protocol_run_attempt CHECK (attempt >= 0),
    CONSTRAINT ck_protocol_run_completed CHECK ((current_state IN ('DONE', 'FAILED')) = (completed_at IS NOT NULL))
);

CREATE INDEX idx_protocol_run_task ON protocol_run (task_id);

CREATE TABLE test_case (
    id              UUID         PRIMARY KEY,
    task_id         UUID         NOT NULL REFERENCES task (id),
    position        INT          NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    description     TEXT         NOT NULL,
    input_data      TEXT         NOT NULL,
    expected_output TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_test_case_type CHECK (type IN ('BASIC', 'EDGE', 'BOUNDARY')),
    CONSTRAINT uq_test_case_position UNIQUE (task_id, position)
);

CREATE TABLE worker_run (
    id                UUID          PRIMARY KEY,
    protocol_run_id   UUID          NOT NULL REFERENCES protocol_run (id),
    worker_name       VARCHAR(100)  NOT NULL,
    model             VARCHAR(100),
    status            VARCHAR(20)   NOT NULL,
    prompt_tokens     INT           NOT NULL,
    completion_tokens INT           NOT NULL,
    started_at        TIMESTAMPTZ   NOT NULL,
    finished_at       TIMESTAMPTZ,

    CONSTRAINT ck_worker_run_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_worker_run_protocol_run ON worker_run (protocol_run_id);

CREATE TABLE solution (
    id                       UUID          PRIMARY KEY,
    worker_run_id            UUID          NOT NULL REFERENCES worker_run (id),
    version                  INT           NOT NULL,
    language                 VARCHAR(20)   NOT NULL,
    source_code              TEXT          NOT NULL,
    claimed_time_complexity  VARCHAR(100),
    claimed_space_complexity VARCHAR(100),
    explanation              TEXT,
    created_at               TIMESTAMPTZ   NOT NULL,

    CONSTRAINT ck_solution_version CHECK (version >= 1),
    CONSTRAINT ck_solution_language CHECK (language IN ('JAVA', 'PHP')),
    CONSTRAINT uq_solution_version UNIQUE (worker_run_id, version)
);

CREATE TABLE execution (
    id           UUID         PRIMARY KEY,
    solution_id  UUID         NOT NULL REFERENCES solution (id),
    test_case_id UUID         NOT NULL REFERENCES test_case (id),
    status       VARCHAR(20)  NOT NULL,
    stdout       TEXT         NOT NULL,
    stderr       TEXT         NOT NULL,
    runtime_ms   BIGINT       NOT NULL,
    memory_mb    INT,
    created_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_execution_status CHECK (status IN (
        'PASS', 'WRONG_ANSWER', 'COMPILE_ERROR', 'RUNTIME_ERROR', 'TIMEOUT', 'RESOURCE_LIMIT'))
);

CREATE INDEX idx_execution_solution ON execution (solution_id);

CREATE TABLE evaluation (
    id              UUID         PRIMARY KEY,
    solution_id     UUID         NOT NULL UNIQUE REFERENCES solution (id),
    status          VARCHAR(20)  NOT NULL,
    reason          TEXT         NOT NULL,
    compiler_output TEXT         NOT NULL,
    passed_tests    INT          NOT NULL,
    total_tests     INT          NOT NULL,
    max_runtime_ms  BIGINT       NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_evaluation_status CHECK (status IN (
        'PASS', 'WRONG_ANSWER', 'COMPILE_ERROR', 'RUNTIME_ERROR', 'TIMEOUT', 'RESOURCE_LIMIT'))
);
