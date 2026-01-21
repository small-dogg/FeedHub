-- QnA 테이블
CREATE TABLE qna (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    member_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    is_secret BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_qna_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);

CREATE INDEX idx_qna_member_id ON qna(member_id);
CREATE INDEX idx_qna_status ON qna(status);
CREATE INDEX idx_qna_created_at ON qna(created_at DESC);

-- QnA 답변 테이블
CREATE TABLE qna_answer (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    qna_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_qna_answer_qna FOREIGN KEY (qna_id) REFERENCES qna(id) ON DELETE CASCADE,
    CONSTRAINT fk_qna_answer_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);

CREATE INDEX idx_qna_answer_qna_id ON qna_answer(qna_id);
