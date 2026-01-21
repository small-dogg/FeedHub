-- Add role column to member table
ALTER TABLE member
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Set user with ID 1 as ADMIN
UPDATE member SET role = 'ADMIN' WHERE id = 1;

CREATE INDEX idx_member_role ON member(role);
