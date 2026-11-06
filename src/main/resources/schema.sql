CREATE TABLE IF NOT EXISTS ticket_draft (
    draft_id VARCHAR(36) PRIMARY KEY,
    draft_json CLOB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS support_ticket (
    ticket_id VARCHAR(36) PRIMARY KEY,
    draft_id VARCHAR(36) NOT NULL UNIQUE,
    system_name VARCHAR(100) NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    category VARCHAR(40) NOT NULL,
    summary VARCHAR(200) NOT NULL,
    actual_result VARCHAR(1000) NOT NULL,
    expected_result VARCHAR(1000) NOT NULL,
    occurred_at_text VARCHAR(100) NOT NULL,
    version VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ticket_draft FOREIGN KEY (draft_id) REFERENCES ticket_draft(draft_id)
);
