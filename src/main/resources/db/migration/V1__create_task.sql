CREATE TABLE task (
    id UUID PRIMARY KEY,
    problem TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    protocol_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(50) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

CONSTRAINT ck_task_language
    CHECK (language IN ('JAVA', 'PHP')),

CONSTRAINT ck_task_protocol_type
    CHECK (protocol_type IN ('PROBLEM_SOLVER')),

CONSTRAINT ck_task_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),

CONSTRAINT ck_task_failure_reason
    CHECK (
        failure_reason IS NULL
        OR failure_reason IN ('TASK_TIMEOUT', 'NO_PASSING_CANDIDATE', 'REPAIR_EXHAUSTED', 'ANALYZER_ERROR', 'INFRASTRUCTURE_ERROR')
    ),

CONSTRAINT ck_task_failure_consistency
    CHECK (
        (status = 'FAILED') = (failure_reason IS NOT NULL)
    )
);

CREATE INDEX idx_task_status ON task (status);