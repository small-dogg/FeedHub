-- Add member_id to tag table for user-specific tags

-- Add member_id column (nullable first for migration)
ALTER TABLE tag ADD COLUMN member_id BIGINT;

-- Migrate existing tags to member_id = 1
UPDATE tag SET member_id = 1;

-- Make member_id NOT NULL after migration
ALTER TABLE tag ALTER COLUMN member_id SET NOT NULL;

-- Add foreign key constraint
ALTER TABLE tag ADD CONSTRAINT fk_tag_member
    FOREIGN KEY (member_id) REFERENCES member(id);

-- Drop old unique constraint on name only
ALTER TABLE tag DROP CONSTRAINT tag_name_key;

-- Add new unique constraint on (member_id, name)
ALTER TABLE tag ADD CONSTRAINT uk_tag_member_name UNIQUE (member_id, name);

-- Add index for member_id
CREATE INDEX idx_tag_member_id ON tag(member_id);
